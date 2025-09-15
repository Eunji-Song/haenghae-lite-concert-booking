package kr.hhplus.be.server.common.repository.concert;

import kr.hhplus.be.server.common.entity.concert.ConcertSeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConcertSeatEntityRepository extends JpaRepository<ConcertSeatEntity, Long> {
    /**
     * 콘서트 날짜와 좌석 번호로 좌석 조회
     */
    @Query("""
            SELECT cs 
            FROM ConcertSeatEntity cs 
            WHERE cs.concertDate.id = :concertDateId 
            AND cs.seatNo = :seatNo
            AND cs.deletedAt IS NULL
            """)
    Optional<ConcertSeatEntity> findByConcertDateIdAndSeatNo(
            @Param("concertDateId") Long concertDateId,
            @Param("seatNo") Long seatNo
    );

    /**
     * 콘서트 날짜별 모든 좌석 조회 (좌석 번호 순)
     */
    @Query("""
            SELECT cs 
            FROM ConcertSeatEntity cs 
            WHERE cs.concertDate.id = :concertDateId
            AND cs.deletedAt IS NULL
            ORDER BY cs.seatNo ASC
            """)
    List<ConcertSeatEntity> findByConcertDateIdOrderBySeatNoAsc(@Param("concertDateId") Long concertDateId);

    /**
     * 특정 가격대 좌석 조회
     */
    @Query("""
            SELECT cs 
            FROM ConcertSeatEntity cs 
            WHERE cs.concertDate.id = :concertDateId
            AND cs.price BETWEEN :minPrice AND :maxPrice
            AND cs.deletedAt IS NULL
            ORDER BY cs.price ASC, cs.seatNo ASC
            """)
    List<ConcertSeatEntity> findByConcertDateIdAndPriceBetween(
            @Param("concertDateId") Long concertDateId,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice
    );

    /**
     * 구역별 좌석 조회
     */
    @Query("""
            SELECT cs 
            FROM ConcertSeatEntity cs 
            WHERE cs.concertDate.id = :concertDateId
            AND cs.section = :section
            AND cs.deletedAt IS NULL
            ORDER BY cs.seatNo ASC
            """)
    List<ConcertSeatEntity> findByConcertDateIdAndSection(
            @Param("concertDateId") Long concertDateId,
            @Param("section") String section
    );
}