package kr.hhplus.be.server.api.clean.application.port.out.reservation;

import kr.hhplus.be.server.api.clean.domain.model.reservation.Reservation;

import java.util.List;
import java.util.Optional;

/**
 * 예약 Repository 포트 - 엔티티가 아닌 도메인 클래스를 사용
 */

public interface ReservationRepository {

    /**
     * 예약 저장
     */
    Reservation save(Reservation reservation);

    /**
     * ID로 예약 조회
     */
    Optional<Reservation> findById(Long id);

    /**
     * 좌석에 활성 예약이 있는지 확인
     * (PENDING 또는 CONFIRMED 상태)
     */
    boolean existsActiveBySeat(Long seatId);

    /**
     * 사용자별 예약 목록 조회
     */
    List<Reservation> findByUserId(Long userId);

    /**
     * 좌석별 활성 예약 조회
     */
    List<Reservation> findActiveBySeat(Long seatId);
}