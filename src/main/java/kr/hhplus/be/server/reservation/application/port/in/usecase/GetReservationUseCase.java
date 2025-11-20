package kr.hhplus.be.server.reservation.application.port.in.usecase;

import kr.hhplus.be.server.reservation.application.port.in.result.ReservationDetailResponse;

import java.util.List;

public interface GetReservationUseCase {
    ReservationDetailResponse get(String userUuid, Long reservationId);
    List<ReservationDetailResponse> getMy(String userUuid);
}