package kr.hhplus.be.server.reservation.application.port.out;

import kr.hhplus.be.server.reservation.domain.model.Reservation;

import java.util.Optional;
import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository {
    Reservation save(Reservation reservation);

    Optional<Reservation> findById(Long id);

    List<Reservation> findByUserId(Long userId);

    boolean isSeatOccupiable(Long seatId);

    Long resolveSeatId(Long concertId, LocalDate eventDate, int seatNo);

    long findSeatPriceBySeatId(Long seatId);

    /** 결제 성공 후 예약 확정 처리 */
    void confirm(Long reservationId, Long userId);
}