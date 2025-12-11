# 캐싱 적용 보고서 (Redis Cache 기반 성능 최적화)

본 문서는 콘서트 예약 서비스에서 조회 성능을 개선하고 DB 부하를 줄이기 위해
적용한 캐싱 전략(Cacheable)의 구조, 동작 방식, 테스트 전략을 정리한 보고서입니다.

---

# 1. 캐시 필요성

조회 API의 호출량은 예약/결제보다 훨씬 많고, 데이터 변경은 드물다.

따라서 캐싱을 적용하면 다음과 같은 이점이 있다:
- DB 부하 감소
- 빠른 응답 속도
- 높은 트래픽에서도 안정적 동작

캐시 적용 대상은 다음 두 API이다:
1. 콘서트 예약 가능 일자 조회
2. 날짜별 좌석 조회

---

#  2. 캐시 적용 대상

## 2.1 콘서트 예약 가능 날짜 조회 (getOpenDates)
- 변경이 거의 없음
- 대부분의 사용자가 가장 먼저 호출하는 API
- 캐시 적합도가 매우 높음

캐시 키:
concert:openDates::{concertId}

---

##  2.2 날짜별 좌석 조회 (getSeats)
- 좌석 번호/가격 등은 정적 데이터 → 캐싱 가능
- 하지만 AVAILABLE / HELD 여부는 실시간 정보 → 캐싱하지 않음

캐시 키:
concert:seats::{concertId}:{date}

---

# 3. Redis Cache 설정 구조

## 3.1 애플리케이션 설정

@EnableCaching  
@SpringBootApplication

캐싱 사용 선언.

---

## 3.2 RedisCacheManager 설정

TTL 5분 적용, NULL 값 캐싱 방지.

---

# 4. 캐시 적용 코드

## 4.1 openDates 캐싱  

Service 레이어에서 Cacheable 사용   
key: concertId  


  
핵심 포인트: dateAdapter.getOpenDates()가 캐싱됨.

---

## 4.2 seats 캐싱

key: concertId + “:” + date   
정적 좌석 정보만 캐싱됨.   
좌석 점유 여부는 ReservationRepository가 별도 조회.   

---

# 5. 테스트 전략

Testcontainers 환경에서는 Redis가 없기에 RedisCacheManager 사용 불가.

그래서 테스트 전용 CacheManager를 overriding함.

Primary CacheManager → ConcurrentMapCacheManager 로 변경.

이렇게 하면 테스트에서 Redis 없이도 캐싱 로직이 정상적으로 동작함.

---
# 6. 캐시 통합 테스트

테스트 포인트:
- 동일 파라미터로 두 번 호출 시 Repository 호출은 1번만 발생해야 한다
- 캐시에 값이 저장되어야 한다
- 반환 값은 동일해야 한다

SpyBean으로 호출 횟수 검증.

---

# 7. 성능 개선 요약

캐시 도입 효과:
- 동일 조회 반복 시 DB 호출 제거 
- 평균 응답 속도 향상
- DB Connection 수 감소
- 트래픽 급등 시 안정성 확보

---

# 8. 결론

Redis Cache를 적용해 조회 API 성능을 크게 개선했으며
트래픽 증가에도 안정적으로 응답 가능한 구조를 확보했다.

좌석 점유 여부와 같이 실시간성이 중요한 부분은 캐싱하지 않아
정확성과 성능 두 가지를 모두 만족하는 구조를 만든다.