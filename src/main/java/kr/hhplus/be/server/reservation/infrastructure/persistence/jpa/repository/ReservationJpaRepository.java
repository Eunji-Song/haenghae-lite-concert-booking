package kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.repository;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, Long> {

    // 좌석의 "현재 활성 점유" 수 → 0이면 점유 가능
    @Query("""
           select count(r)
             from ReservationEntity r
            where r.seatId = :seatId
              and r.isActive = true
           """)
    long countActiveBySeatId(@Param("seatId") Long seatId);

    List<ReservationEntity> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    // 단건 강제 만료 (선택 API): PENDING -> EXPIRED + isActive=false
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update ReservationEntity r
              set r.status = :expired,
                  r.expiredAt = :now,
                  r.isActive = false
            where r.id = :id
              and r.status = 'PENDING'
              and r.isActive = true
           """)
    int forceExpire(@Param("id") Long id,
                    @Param("expired") ReservationStatus expired,
                    @Param("now") LocalDateTime now);

    // 만료 일괄 처리(선택): 홀드 시간 지난 PENDING들을 일괄 만료
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update ReservationEntity r
              set r.status = 'EXPIRED',
                  r.expiredAt = :now,
                  r.isActive = false
            where r.status = 'PENDING'
              and r.isActive = true
              and r.holdExpiresAt <= :now
           """)
    int bulkExpireStaled(@Param("now") LocalDateTime now);

    @Query("select s.price from ConcertSeatEntity s where s.id = :seatId")
    Long findSeatPriceBySeatId(@Param("seatId") Long seatId);
}