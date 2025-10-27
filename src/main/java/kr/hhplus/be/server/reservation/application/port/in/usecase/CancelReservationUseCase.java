package kr.hhplus.be.server.reservation.application.port.in.usecase;

import kr.hhplus.be.server.reservation.application.port.in.result.CancelReservationResponse;

public interface CancelReservationUseCase {
    void cancel(String userUuid, Long reservationId);
}