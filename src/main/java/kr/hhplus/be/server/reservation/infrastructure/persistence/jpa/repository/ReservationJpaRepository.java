package kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.repository;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reservation JPA Repository.
 *
 * 예약 엔티티의 조회, 만료 처리 등의 DB 작업을 담당한다.
 * 좌석 점유 여부 조회, 조건부 만료 업데이트, 만료 스케줄러용 일괄 업데이트,
 * 좌석 가격 조회 기능 등을 제공한다.
 */
public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, Long> {

    /**
     * 특정 좌석(seatId)에 대해 현재 활성(active) 상태의 예약 개수를 조회한다.<br>
     * 활성 예약이 0이면 해당 좌석은 점유 가능하다.
     *
     * @param seatId 좌석 ID
     * @return 활성 예약 개수
     */
    @Query("""
            SELECT COUNT(r)
              FROM ReservationEntity r
             WHERE r.seatId = :seatId
               AND r.isActive = true
            """)
    long countActiveBySeatId(@Param("seatId") Long seatId);

    /**
     * 특정 사용자가 생성한 모든 예약을 생성일 역순으로 조회한다.
     *
     * @param userId 사용자 ID
     * @return 예약 리스트
     */
    List<ReservationEntity> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    /**
     * 단일 예약을 강제로 만료(EXPIRED) 상태로 변경한다.<br>
     * 상태가 PENDING이고 active 상태인 경우에만 업데이트된다.
     *
     * @param id      예약 ID
     * @param expired 변경할 상태 (EXPIRED)
     * @param now     만료 처리 시각
     * @return 영향받은 row 수 (성공 시 1, 실패 시 0)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ReservationEntity r
               SET r.status = :expired,
                   r.expiredAt = :now,
                   r.isActive = false
             WHERE r.id = :id
               AND r.status = 'PENDING'
               AND r.isActive = true
            """)
    int forceExpire(@Param("id") Long id,
                    @Param("expired") ReservationStatus expired,
                    @Param("now") LocalDateTime now);

    /**
     * 홀드 만료 시간이 지난 모든 PENDING 예약을 EXPIRED 상태로 변경한다.<br>
     * 스케줄러에서 호출되는 일괄 업데이트용 쿼리다.
     *
     * @param now 만료 조건 기준 시각
     * @return 만료 처리된 row 개수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ReservationEntity r
               SET r.status = 'EXPIRED',
                   r.expiredAt = :now,
                   r.isActive = false
             WHERE r.status = 'PENDING'
               AND r.isActive = true
               AND r.holdExpiresAt <= :now
            """)
    int bulkExpireStaled(@Param("now") LocalDateTime now);

    /**
     * 좌석 ID를 기반으로 좌석 가격을 조회한다.
     *
     * @param seatId 좌석 ID
     * @return 좌석 가격
     */
    @Query("SELECT s.price FROM ConcertSeatEntity s WHERE s.id = :seatId")
    Long findSeatPriceBySeatId(@Param("seatId") Long seatId);
}