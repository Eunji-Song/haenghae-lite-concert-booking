package kr.hhplus.be.server.reservation.application.port.out;

import kr.hhplus.be.server.reservation.domain.model.Reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {

    /** 예약 정보를 저장하거나 갱신한다. (PERSIST / UPDATE). 유니크 충돌 시 예외 발생 가능 */
    Reservation save(Reservation reservation);

    /** 예약 ID로 예약 정보를 조회한다. */
    Optional<Reservation> findById(Long id);

    /** 특정 사용자 ID의 예약 목록을 조회한다. */
    List<Reservation> findByUserId(Long userId);

    /** 좌석이 현재 점유 가능한지(활성 점유 레코드가 없는지) 확인한다. */
    boolean isSeatOccupiable(Long seatId);

    /** 공연ID+날짜+좌석번호로 좌석ID를 조회한다. */
    Long resolveSeatId(Long concertId, LocalDate eventDate, int seatNo);

    /** 좌석 가격 조회 */
    long findSeatPriceBySeatId(Long seatId);

    /** 결제 성공 후 예약을 확정(PENDING→CONFIRMED) 처리한다. */
    void confirm(Long reservationId, Long userId);

    /** 좌석 행을 비관적 락으로 잠근다(경쟁 진입 방지). */
    void lockSeat(Long seatId);
}