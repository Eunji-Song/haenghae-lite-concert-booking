package kr.hhplus.be.server.product.infrastructure.jpa.repository;

import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertSeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConcertSeatJpaRepository extends JpaRepository<ConcertSeatEntity, Long> {

    /**
     * concert_date_id + seat_no 로 단일 좌석 조회 (DDL: UNIQUE (concert_date_id, section, seat_no)지만 section 미사용이면 seat_no만)
     */
    Optional<ConcertSeatEntity> findByConcertDate_IdAndSeatNo(Long concertDateId, int seatNo);

    /**
     * 특정 공연일(concert_date_id)의 모든 좌석
     */
    List<ConcertSeatEntity> findAllByConcertDate_IdOrderBySeatNoAsc(Long concertDateId);

    /**
     * 공연ID + 날짜로 좌석들 조회 (concert_dates 조인 필요)
     */
    @Query("""
                select s
                from ConcertSeatEntity s
                join s.concertDate d
                where d.concert.id = :concertId
                  and d.eventDate = :eventDate
                order by s.seatNo asc
            """)
    List<ConcertSeatEntity> findAllByConcertIdAndEventDate(Long concertId, LocalDate eventDate);

    /**
     * 공연ID + 날짜 + 좌석번호로 단일 좌석 조회 (조인 버전)
     */
    @Query("""
                select s
                from ConcertSeatEntity s
                join s.concertDate d
                where d.concert.id = :concertId
                  and d.eventDate = :eventDate
                  and s.seatNo = :seatNo
            """)
    Optional<ConcertSeatEntity> findOneByConcertIdAndEventDateAndSeatNo(Long concertId, LocalDate eventDate, int seatNo);
}