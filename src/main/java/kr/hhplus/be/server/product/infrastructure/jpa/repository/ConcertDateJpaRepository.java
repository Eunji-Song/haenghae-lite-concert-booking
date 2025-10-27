package kr.hhplus.be.server.product.infrastructure.jpa.repository;

import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertDateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * concert_dates 테이블 JPA 리포지토리.
 *
 * 스키마:
 *  - PK: id (BIGINT)
 *  - FK: concert_id -> concerts.id
 *  - UNIQUE: (concert_id, event_date)
 *  - 컬럼: event_date, venue_name, is_open, created_at, updated_at, deleted_at
 */
public interface ConcertDateJpaRepository extends JpaRepository<ConcertDateEntity, Long> {

    /**
     * 공연 ID와 날짜로 단일 일자 조회 (UNIQUE (concert_id, event_date))
     */
    Optional<ConcertDateEntity> findByConcert_IdAndEventDate(Long concertId, LocalDate eventDate);

    /**
     * 특정 공연의 '예매 오픈'된 일자들을 날짜 오름차순으로 조회
     */
    List<ConcertDateEntity> findAllByConcert_IdAndOpenTrueOrderByEventDateAsc(Long concertId);

    /**
     * 공연 ID와 날짜 조합이 존재하는지 여부
     */
    boolean existsByConcert_IdAndEventDate(Long concertId, LocalDate eventDate);

    /**
     * 특정 공연의 모든 일자 조회 (오픈 여부 무관, 날짜 오름차순)
     */
    List<ConcertDateEntity> findAllByConcert_IdOrderByEventDateAsc(Long concertId);

    /**
     * 공연 ID와 날짜로 concert_date의 PK 조회
     */
    Optional<Long> findIdByConcertIdAndEventDate(Long concertId, LocalDate eventDate);
}