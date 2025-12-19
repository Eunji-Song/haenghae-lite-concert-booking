# 🎤 콘서트 빠른 매진 랭킹 설계 보고서 (Redis Sorted Set 기반)

본 문서는 콘서트 예약 서비스에서 가장 빠르게 매진된 콘서트를 순위화하기 위해
Redis Sorted Set을 기반으로 설계·구현한 월별 매진 랭킹 시스템의 구조와 동작 방식, 테스트 전략을 정리한 보고서입니다.

---

# 랭킹 기능 필요성

콘서트 서비스에서 매진 속도는 인기 지표로 활용될 수 있으며,
사용자에게 제공되는 “이번 달 가장 빨리 매진된 콘서트 Top N” 같은 콘텐츠는 서비스 경쟁력을 강화한다.

그러나 매진 랭킹은 다음과 같은 특성을 가진다:
- 매진 이벤트는 적지만 조회 API 호출량이 크다 
- 매진 시각은 발생하면 수정되지 않는 단발성 이벤트 
- 빠른 응답 속도를 요구하는 고트래픽 조회 API 

따라서 Redis 기반 정렬 구조(Sorted Set)를 활용해 랭킹을 관리하면 다음과 같은 이점이 있다:
- 실시간에 가까운 조회 속도(O(log N) 삽입 / O(log N + K) 조회)
- 정렬/순위 계산을 Redis가 담당 → 서버 부담 감소
- TTL로 월별 데이터 자동 정리 가능

---

# 2. 랭킹 기준

## 2.1 매진 판정 기준

좌석 상태 중 전체 좌석이 ASSIGNED(결제 완료) 되었을 때를 매진으로 기록한다.
- RESERVED(임시 홀드)는 만료될 수 있어 매진으로 보기 부적절
- ASSIGNED는 되돌릴 수 없으며 최종 매진 상태로 간주 가능
- 전체 좌석 상태가 ASSIGNED일 때 “매진 시각”으로 기록

---

## 2.2 score 산정 기준

Redis Sorted Set은 score 기준으로 정렬되므로 다음 규칙을 따른다.

score = 매진 시각(KST 기반) → UTC epoch milli 변환 값

작을수록 더 이른 시각 → 더 빠른 매진 → 더 높은 랭킹.

---

# 3. Redis Sorted Set 구조

## 3.1 월별 키 구조

월 단위로 랭킹을 분리해 데이터 누적과 메모리 낭비를 방지한다.

```java
ranking:concert:soldout:{yyyy-MM}
예: ranking:concert:soldout:2025-12
```

- member: concertId (문자열)
- score: soldOutAt(epoch milli)
- value: 매진 완료된 콘서트 ID

## 3.2 TTL 정책

각 월별 랭킹 키에 31일 TTL 적용:
- 지난달 랭킹은 더 이상 필요하지 않음
- Redis 메모리 사용량 자동 정리
- 운영 비용 감소

---

# 4. 매진 랭킹 기록 로직

콘서트 좌석이 마지막으로 결제되어 최종 매진이 감지되면 다음 로직 수행.

## 4.1 recordSoldOut(concertId, soldOutAt)

1.	soldOutAt(LocalDateTime, KST) → ZonedDateTime 변환
2. YearMonth 추출
3.	랭킹 키 생성
4.	ZADD로 score(member) 삽입
5.	TTL 설정

### Redis 명령어 예시
```java
ZADD ranking:concert:soldout:2025-12 1733030400000 "42"
EXPIRE ranking:concert:soldout:2025-12 2678400
```

# 5. 매진 랭킹 조회 API

## 5.1 API 엔드포인트

```java
GET /api/v1/rankings/concerts/soldout?month=2025-12&limit=10
```

## 5.2 기능 설명
- month 쿼리가 없으면 운영 서버 Clock 기준(KST)의 현재 월 사용
- limit만큼 Top N 정렬 결과를 반환
- score(epoch milli)를 다시 KST 기반 LocalDateTime으로 변환하여 응답

---

# 6. 주요 코드 구성

## 6.1 Service – RedisConcertRankingService

핵심 기능:
- 월별 키 생성
- ZADD로 랭킹 기록
- ZRANGE WITHSCORES로 Top N 조회
- score → LocalDateTime(KST) 변환
- TTL 설정

## 6.2 Controller – ConcertRankingController

핵심 기능:
- Clock 기반 YearMonth 파싱
- 잘못된 month 형식 처리
- rank 계산 후 DTO 변환
- 조회 파라미터 limit 기본값 적용

---

# 7. 테스트 전략

Redis 연동 없이도 로직 검증이 가능하도록 Mockito 기반 단위 테스트를 구성했다.

## 7.1 recordSoldOut 테스트
- ZADD 호출 여부 검증
- key 생성 규칙 검증
- epoch milli score 검증(KST 타임존)
- TTL 설정 검증

## 7.2 getFastSoldOutTopN 테스트
- ZSET rangeWithScores 호출 검증
- score → LocalDateTime(KST) 변환 검증
- limit ≤ 0 → 빈 리스트 처리
- null Month → 예외 검증
- empty result 처리

테스트에서는 ZoneId.of(“Asia/Seoul”)을 명시해 시차 문제를 제거했다.

---

# 8. 성능 요약

Redis Sorted Set을 활용한 월별 랭킹 구조는 다음 장점을 제공한다:
- 조회 성능 우수 (ms 단위 응답)
- 서버 부하 감소 (정렬·순위 계산을 Redis가 처리)
- TTL을 통한 메모리 자동 관리
- 월별 데이터 분리 → 구조적 안정성 확보
- 실시간 매진 기록 반영 가능(이벤트 기반 처리 시)

---

# 9. 결론

본 시스템은 Redis Sorted Set을 이용한 효율적인 콘서트 매진 랭킹 기능을 제공하며,
- 매진 판정 기준의 명확성
- 월별 키 구조의 단순함
- TTL을 통한 메모리 관리
- 고성능 조회 API
- 테스트 가능성과 유지보수성

모든 면에서 안정적이고 확장성 있는 구조를 갖추고 있다.

향후 좌석 결제 이벤트 기반의 자동 기록, 인기 콘서트 추천 기능 등도 쉽게 확장 가능하다. 