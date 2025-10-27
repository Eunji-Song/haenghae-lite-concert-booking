package kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.repository;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationEntityRepository extends JpaRepository<ReservationEntity, Long> {

    @Query("""
           select count(r) from ReservationEntity r
           where r.seat.id = :seatId
             and (
               r.status = kr.hhplus.be.server.common.enums.ReservationStatus.CONFIRMED
               or (r.status = kr.hhplus.be.server.common.enums.ReservationStatus.PENDING and r.holdExpiresAt > :now)
             )
           """)
    long countActiveOccupancy(@Param("seatId") Long seatId, @Param("now") LocalDateTime now);

    List<ReservationEntity> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ReservationEntity r set r.status = :expired, r.expiredAt = :now where r.id = :id and r.status = 'PENDING'")
    int forceExpire(@Param("id") Long id,
                    @Param("expired") ReservationStatus expired,
                    @Param("now") LocalDateTime now);
}