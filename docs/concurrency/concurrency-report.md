# 동시성 문제 해결 보고서 (동시성 시나리오 및 해결 전략)

--- 

## 지갑 동시 차감 문제 (Wallet Debit Concurrency)

### 문제 상황
- 여러 스레드가 동시에 동일 사용자 지갑을 차감할 때 잔액이 음수로 내려가거나, 중복 차감되는 Lost Update 문제가 발생 가능 
- 초기 잔액 100,000원
    - Thread A: 30,000원 차감
    - Thread B: 30,000원 차감
    - Thread C: 30,000원 차감
    - 기대: 최대 3번 성공
    - 잘못된 구현: 4번 이상 성공하거나 음수 잔액 발생

### 원인
- 단순 find → update 로직은 트랜잭션 간 경합이 발생하여 동일한 잔액을 중복 읽을 가능성 존재
- "선착순" 형태의 비즈니스에서는 정합성이 극도로 중요

### 해결 전략
####  1. SELECT FOR UPDATE 사용 (비관적 락)
지갑 차감 로직에서는 중복 차감 위험이 크기 때문에 비관적 락이 적합합니다.

#### 적용 코드 (WalletAdapter)
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<WalletAccountEntity> findByIdForUpdate(Long userId);
```

WalletPort.debit() 내부에서 동일 row가 락 걸리므로 다른 스레드는 해당 행 업데이트가 완료될 때까지 대기 

### 2. 테스트: WalletDebitConcurrencyTest
- 10개의 스레드가 동시에 차감 요청
- 성공 횟수 ≤ (전체 잔액 / 차감 금액)
- 음수 잔액 발생 X
- 모든 트랜잭션 정합성 보장

---

## 동시 좌석 예약 문제 (Seat Reservation Concurrency)

### 문제 상황
- 동일한 좌석에 동시에 예약 요청 시 중복 예약되는 Race Condition
- 예: User1, User2 동시 요청 → 둘 다 예약 성공

### 원인
- 좌석 점유 여부를 확인하는 시점과 업데이트하는 시점 사이에 경쟁 발생

### 해결 전략
### 1. 조건부 UPDATE (WHERE + 상태 검증)
좌석은 트래픽이 매우 높을 수 있으므로 락보다 성능이 중요

### 적용 방식 (조건부 UPDATE)
```java
UPDATE reservation
SET status = 'PENDING'
WHERE id = :id
AND is_active = true
```

상태가 여전히 ACTIVE인 경우에만 점유 가능.  
동시에 UPDATE할 경우 정확히 1명만 성공.

### 장점
- 트래픽이 많아도 락 대기 시간이 없음
- 실패한 요청은 즉시 실패 응답 (idempotent)

### 테스트
- 멀티스레드 환경에서 단 1개의 스레드만 성공하는지 검증

---

## PENDING 예약 만료 스케줄러 (Hold Timeout)

### 문제 상황
- 예약은 `holdSeconds` 동안만 임시 점유됨
- 결제가 되지 않은 오래된 PENDING 예약을 회수해야 함
- 스케줄러 실행 시 여러 예약을 동시에 만료시켜야 할 수 있음

### 해결 전략
### 1. 조건부 UPDATE 기반 일괄 만료
```java
UPDATE reservation
SET status = 'EXPIRED',
    expiredAt = :now,
    isActive = false
WHERE status = 'PENDING'
AND isActive = true
AND holdExpiresAt <= :now
```

- 스레드 충돌 없음
- 대량 UPDATE 처리 가능

### 테스트: ReservationExpireSchedulerTest
- 만료된 예약이 올바르게 EXPIRED로 전환되는지 확인

---

## 결제 동시성 문제 (Payment Concurrency)

### 문제 상황
- 결제가 여러 번 중복 처리되면 크리티컬한 손실이 발생
- 특히 지갑 포인트 차감 → 결제 저장 → 상태 CONFIRMED 흐름이 매우 중요

### 원인
- Payment 존재 여부 확인과 저장 과정 간 경합
- 멱등성 키(idempotency-key) 없이 처리하면 중복 결제 발생

### 해결 전략
### 1. Idempotency-Key 기반 멱등성 보장
```java
if (paymentRepository.existsByIdempotencyKey(idem)) {
    return new PaymentResponse(null, reservationId, true);
}
```

### 2. SELECT FOR UPDATE 대신 기존 흐름 유지
- 예약 확정(reservation.confirm)은 자동으로 낙관적 락 (version) 적용
- Payment 저장은 idempotency key로 완전 방지

### 3. 전체 흐름 통합 테스트 수행
- PayForReservationFlowTest에서 end-to-end 검증

---

## 3. 적용 기술 요약

| 기능 영역 | 적용 전략 | 목적 |
|-----------|-----------|-------|
| 지갑 차감 | 비관적 락(PESSIMISTIC_WRITE) | 정합성 최우선, 중복 차감 방지 |
| 좌석 예약 | 조건부 UPDATE | 고트래픽 대비 성능 최적화 |
| 예약 만료 스케줄러 | 조건부 UPDATE + TTL 검증 | 오래된 PENDING 정리 |
| 결제 처리 | 멱등성 키(Idempotency) + 낙관적 락 | 중복 결제 방지 |
| 예약 확정 | JPA @Version 기반 낙관적 락 | 상태 전이 충돌 방지 |

---

## 4. 최종 결과 요약

- 모든 동시성 시나리오에 대해 멀티스레드 테스트 및 통합 테스트 통과
- 트랜잭션 격리 수준 및 락 전략을 도메인별로 다르게 적용하여 성능 & 정합성 동시 확보
- 대기열 → 예약 → 결제 전체 흐름이 안정적으로 동작함을 확인



