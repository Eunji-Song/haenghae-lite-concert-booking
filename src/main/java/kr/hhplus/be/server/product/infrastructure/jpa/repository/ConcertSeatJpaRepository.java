package kr.hhplus.be.server.product.infrastructure.jpa.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertSeatEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConcertSeatJpaRepository extends JpaRepository<ConcertSeatEntity, Long> {

    /** 특정 공연일(concert_date_id)에서 좌석번호(seat_no)로 단일 좌석 조회 */
    Optional<ConcertSeatEntity> findByConcertDate_IdAndSeatNo(Long concertDateId, int seatNo);

    /** 특정 공연일(concert_date_id)의 모든 좌석을 좌석번호 오름차순으로 조회 */
    List<ConcertSeatEntity> findAllByConcertDate_IdOrderBySeatNoAsc(Long concertDateId);

    /** 공연ID + 날짜(LocalDate)에 해당하는 모든 좌석을 좌석번호 오름차순으로 조회 (concert_dates 조인) */
    @Query("""
            select s
              from ConcertSeatEntity s
              join s.concertDate d
             where d.concert.id = :concertId
               and d.eventDate = :eventDate
             order by s.seatNo asc
           """)
    List<ConcertSeatEntity> findAllByConcertIdAndEventDate(Long concertId, LocalDate eventDate);

    /** 공연ID + 날짜 + 좌석번호로 단일 좌석 조회 (concert_dates 조인) */
    @Query("""
            select s
              from ConcertSeatEntity s
              join s.concertDate d
             where d.concert.id = :concertId
               and d.eventDate = :eventDate
               and s.seatNo = :seatNo
           """)
    Optional<ConcertSeatEntity> findOneByConcertIdAndEventDateAndSeatNo(Long concertId, LocalDate eventDate, int seatNo);

    /** 좌석 행을 비관적 쓰기 락(PESSIMISTIC_WRITE)으로 잠금 — 동시 예약 경쟁 시 직렬화 보장 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ConcertSeatEntity s where s.id = :seatId")
    ConcertSeatEntity lockById(@Param("seatId") Long seatId);

    /** 좌석ID로 현재 좌석 가격만 빠르게 조회 (컬럼만 선택) */
    @Query("select s.price from ConcertSeatEntity s where s.id = :seatId")
    Long findPriceById(@Param("seatId") Long seatId);
}