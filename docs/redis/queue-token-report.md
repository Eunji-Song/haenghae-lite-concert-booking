# 🎟 Redis 기반 대기열 토큰 설계 및 구현 보고서

본 문서는 콘서트 예약 서비스에서 **동시 접속 트래픽을 제어하고**
공정한 순서를 보장하기 위해 Redis를 이용해 설계·구현한  
**대기열 토큰(Queue Token) 시스템**의 구조, 동작 방식, 만료/승급 전략 및 테스트 전략을 정리한 보고서입니다.

---

## 1. 대기열 기능 필요성

콘서트 티켓 오픈 시 다음과 같은 문제가 발생한다.

- 짧은 시간에 수천~수만 명의 동시 접속
- DB 직접 접근 시 커넥션 고갈
- 선착순 처리의 공정성 보장 필요
- 결제/예약 과정에서 일정 인원만 서비스 진입 허용 필요

따라서 다음 요구사항을 만족하는 대기열 시스템이 필요하다.

- **동시 활성 사용자 수 제한**
- **선착순 대기 순서 보장**
- **토큰 만료 및 자동 승급 처리**
- **서버 확장에 영향을 받지 않는 중앙 집중식 상태 관리**

이를 위해 Redis 기반의 대기열 토큰 구조를 설계하였다.

---

## 2. 대기열 토큰 설계 개요

### 2.1 핵심 개념

대기열은 두 가지 상태의 토큰으로 구성된다.

| 상태 | 설명 |
|----|----|
| ACTIVE | 현재 서비스 진입이 허용된 사용자 |
| ISSUED | 대기열에 대기 중인 사용자 |

정책:
- ACTIVE 최대 인원: **50명**
- ACTIVE 토큰 TTL: **10분**
- WAITING 토큰 TTL: **1시간**

---

## 3. Redis 자료구조 설계

### 3.1 Key 구조

```
queue:active:{concertId}        // ZSET (score = expireAtMillis)
queue:waiting:{concertId}       // ZSET (score = issuedAtMillis)
queue:token:{token}             // HASH (토큰 메타정보)
queue:token-id:{concertId}:{userUuid} // STRING (idempotency)
queue:concerts                  // SET (스케줄러 순회용)
```

---

## 4. 토큰 발급 로직

토큰 발급은 Lua Script를 통해 Redis에서 원자적으로 처리된다.

주요 처리 단계:
1. 사용자-콘서트 매핑 확인 (중복 방지)
2. ACTIVE 슬롯 여유 확인
3. ACTIVE 또는 WAITING 상태로 토큰 발급
4. TTL 설정 및 상태 저장

---

## 5. 승급(promote) 로직

- 만료된 ACTIVE 토큰 정리
- ACTIVE 여유 슬롯 계산
- WAITING 토큰을 선착순(ZPOPMIN)으로 ACTIVE 승급
- 토큰 상태 및 TTL 갱신

---

## 6. 만료 처리 전략

- ACTIVE: ZSET score 기반 만료
- WAITING: Redis TTL 기반 자동 제거
- promote 및 issue 시점에 만료 정리 수행

---

## 7. Clock 기반 테스트 설계

- Clock을 주입받아 시간 의존 로직 분리
- 테스트에서는 MutableClock 사용
- TTL 만료 및 승급 시나리오를 시간 점프로 재현

---

## 8. 테스트 전략 요약

- redis-test 프로파일 사용
- 실제 Redis 연동 테스트
- 스케줄러 비활성화
- 단계별 Redis 상태 로그 출력

---

## 9. 결론

본 Redis 기반 대기열 시스템은 다음을 보장한다.

- 공정한 선착순 처리
- 고트래픽 환경에서 안정성
- 만료 및 자동 승급 처리
- 테스트 가능성과 확장성

콘서트 예약과 같은 고부하 서비스에 적합한 구조이다.
