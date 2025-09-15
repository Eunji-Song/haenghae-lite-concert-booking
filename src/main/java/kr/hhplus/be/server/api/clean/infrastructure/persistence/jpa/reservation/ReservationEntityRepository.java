package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.reservation;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ReservationEntity JPA Repository
 */
public interface ReservationEntityRepository extends JpaRepository<ReservationEntity, Long> {

    /**
     * 사용자별 예약 목록 조회 (최신순)
     */
    List<ReservationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 좌석에 활성 예약이 있는지 확인
     * PENDING 또는 CONFIRMED 상태인 예약이 있으면 true
     */
    @Query("""
                select (count(r) > 0)
                from ReservationEntity r
                where r.concertSeats.id = :seatId
                  and r.status in :statuses
                  and r.deletedAt is null
            """)
    boolean existsActiveBySeatId(@Param("seatId") Long seatId,
                                 @Param("statuses") List<ReservationStatus> statuses);

    /**
     * 좌석별 활성 예약 목록 조회
     */
    @Query("""
            SELECT r 
            FROM ReservationEntity r 
            WHERE r.concertSeats.id = :seatId 
            AND r.status in :statuses
            AND r.deletedAt IS NULL
            ORDER BY r.createdAt DESC
            """)
    List<ReservationEntity> findActiveBySeatId(@Param("seatId") Long seatId, @Param("statuses") List<ReservationStatus> statuses);

    /**
     * 만료된 PENDING 예약 조회 (배치 처리용)
     */
    @Query("""
            SELECT r 
            FROM ReservationEntity r 
            WHERE r.status = 'PENDING' 
            AND r.holdExpiresAt < CURRENT_TIMESTAMP
            AND r.deletedAt IS NULL
            """)
    List<ReservationEntity> findExpiredPendingReservations();

    @Query("""
                select case when count(r) > 0 then true else false end
                from ReservationEntity r
                where r.concertSeats.id = :seatId
                  and (
                    r.status = 'CONFIRMED'
                    or (r.status = 'PENDING' and r.holdExpiresAt > :now)
                  )
            """)
    boolean existsActiveBySeat(@Param("seatId") Long seatId, @Param("now") LocalDateTime now);
}