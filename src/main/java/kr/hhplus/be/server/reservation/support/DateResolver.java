package kr.hhplus.be.server.reservation.support;

import kr.hhplus.be.server.common.exception.concert.ConcertDateNotFoundException;
import kr.hhplus.be.server.product.domain.repository.ConcertDateRepository;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertDateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 공연 ID + 날짜(LocalDate) → concert_dates.id 를 조회/캐싱하는 Resolver
 */
@Component
@RequiredArgsConstructor
public class DateResolver {

    private final ConcertDateJpaRepository jpaRepository;

    // 캐시: (concertId + ":" + date) → dateId
    private final Map<String, Long> cache = new ConcurrentHashMap<>();

    public Long resolveDateId(Long concertId, LocalDate eventDate) {
        String key = concertId + ":" + eventDate;

        return cache.computeIfAbsent(key, k ->
                jpaRepository.findIdByConcertIdAndEventDate(concertId, eventDate)
                        .orElseThrow(ConcertDateNotFoundException::new)
        );
    }

    /** 필요 시 캐시 초기화용 */
    public void clearCache() {
        cache.clear();
    }
}