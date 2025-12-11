# 동시성 문제 해결 보고서 (Distributed Lock + DB Lock + Idempotency 기반)

본 문서는 콘서트 예약 서비스에서 발생 가능한 주요 동시성 문제를 분석하고,  
이를 해결하기 위해 적용한 **Redis 기반 분산락**, **DB 비관적 락**,  
**조건부 UPDATE**, **JPA 낙관적 락(@Version)**, **Idempotency-Key 기반 멱등성** 전략을 모두 통합하여 정리한 문서이다.

---

# 1. 문제 정의

콘서트 예약 서비스는 다음과 같은 동시성 시나리오에 매우 취약하다:

## 1.1 지갑 동시 차감 문제 (Wallet Debit)
- 여러 요청이 동시에 동일 사용자의 지갑을 차감
- 음수 잔액 또는 Lost Update 위험

## 1.2 동일 좌석 동시 예약 문제 (Seat Reservation)
- 다수 사용자가 같은 좌석을 동시에 클릭
- 단 1명만 성공해야 하지만 Race Condition 존재

## 1.3 예약 PENDING 만료 문제 (Hold Timeout)
- 예약 홀드 기간 경과 시 스케줄러가 동시에 실행될 수 있음

## 1.4 결제 중복 처리 문제 (Payment Idempotency)
- 동일 결제가 여러 번 성공하는 위험

---

# 2. 원인 분석

| 원인 | 설명 |
|------|------|
| **Row-level 경쟁** | 동일 row 업데이트로 충돌 |
| **Race Condition** | 점유 여부 확인 ↔ INSERT/UPDATE 간 타이밍 문제 |
| **다중 서버 환경** | 인스턴스 간 요청 순서 보장 불가 |
| **멱등성 미적용** | 중복 결제 가능 |

---

# 3. 해결 전략 (Summary)

| 문제 영역 | 적용 전략 | 이유 |
|----------|-----------|------|
| 지갑 차감 | **DB 비관적 락(PESSIMISTIC_WRITE)** | 정합성 최우선 |
| 좌석 예약 | **Redis 분산락 + DB 비관적 락 병행** | 다중 서버 안정성 확보 |
| 예약 만료 | **조건부 UPDATE** | 락 없이 고성능 처리 |
| 결제 | **Idempotency-Key + 낙관적 락** | 중복 결제 차단 |
| 예약 확정 | **@Version** | 상태 전이 Race Condition 방지 |

---

# 4. Redis 기반 분산락 설계

## 4.1 SETNX + TTL 방식
좌석별 고유 key로 분산락 생성:

```
SETNX lock:{concertId}:{date}:{seatNo}
EX seconds
```

## 4.2 소유권 보장 Lua 스크립트 unlock 방식
Redis 연결 문제 / TTL 만료 시에도 안전한 unlock 보장:

```lua
if redis.call("get", KEYS[1]) == ARGV[1] then
  return redis.call("del", KEYS[1])
else
  return 0
end
```

## 4.3 적용 범위
- 좌석 예약
- 예약 단건 로직 수행

---

# 5. 좌석 예약 동시성 해결 전략

## 5.1 처리 흐름 요약
### 1) Redis 분산락
- 동일 좌석에 대한 병렬 접근 차단

### 2) DB 비관적 락
좌석 row에 대해:

```java
@Lock(PESSIMISTIC_WRITE)
void lockSeat(Long seatId);
```

두 단계 락을 결합하여 다음 문제를 해결함:
- 다중 서버일 때도 안전
- Redis 장애 시에도 DB 락이 fallback 역할 수행

---

# 6. 지갑 동시 차감 해결 전략

## 6.1 SELECT FOR UPDATE 기반
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<WalletAccountEntity> findByIdForUpdate(Long userId);
```

### 효과
- 잔액 음수 방지
- Lost Update 완전 차단
- 동시 다건 요청에도 선착순으로 직렬화 처리

---

# 7. 예약 PENDING 만료 스케줄러 해결 전략

## 7.1 조건부 UPDATE 방식

```sql
UPDATE reservation
SET status = 'EXPIRED', expired_at = :now, is_active = false
WHERE status = 'PENDING'
  AND is_active = true
  AND hold_expires_at <= :now;
```

장점:
- 성능 매우 좋음
- 락 필요 없음
- 대량 업데이트에 유리

---

# 8. 결제 동시성 해결 전략 (Idempotency + 낙관적 락)

## 8.1 멱등성 키 Idempotency-Key
```java
if (paymentRepository.existsByIdempotencyKey(key)) {
    return previousResult;
}
```

## 8.2 예약 확정 시 낙관적 락(@Version)
- 동시에 두 결제 요청이 들어와도 1건만 CONFIRMED 가능
- Version 충돌 발생 시 자연스럽게 실패 처리

---

# 9. 통합 테스트 결과

통과한 테스트 목록:

| 테스트명 | 검증 내용 |
|----------|-----------|
| **ConcurrentSeatReservationTest** | 좌석 예약은 단 1명만 성공 |
| **WalletDebitConcurrencyTest** | 지갑 음수 방지 및 동시 차감 안전성 확인 |
| **PayForReservationFlowTest** | 예약→결제 end-to-end 성공 |
| **BookingFlowIntegrationTest** | 분산락 + DB락 + 결제까지 전체 플로우 안정성 |
| **ReservationExpireSchedulerTest** | PENDING 만료 스케줄 정상 확인 |

---

# 10. 최종 결론

본 프로젝트는 다음 기술을 조합하여 **고부하 환경에서도 안전한 동시성 제어**를 실현하였다:

✓ Redis 분산락 기반 서버 간 경쟁 제어  
✓ DB 비관적 락 기반 로우 단위 정합성 보장  
✓ JPA 낙관적 락 기반 상태 전이 충돌 방지  
✓ Idempotency 기반 결제 중복 방지  
✓ 조건부 UPDATE 기반 스케줄러 성능 최적화  

이를 통해 **대기열 → 예약 → 지갑 → 결제 → 확정**의 전체 흐름이  
정확하고 안정적으로 동작함이 검증되었다.

---

# 11. Appendix: 실제 적용된 좌석 예약 흐름

```
Client → Redis Lock → DB Lock → 좌석 점유 검증 → Reservation 생성(PENDING)
```

```
Redis Lock 해제
```

```
Payment 요청 → Idempotency 검증 → Wallet 차감(DB Lock) → Reservation CONFIRMED(@Version)
```

---