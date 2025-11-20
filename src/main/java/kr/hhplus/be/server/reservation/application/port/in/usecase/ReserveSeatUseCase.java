package kr.hhplus.be.server.reservation.application.port.in.usecase;

import kr.hhplus.be.server.reservation.application.port.in.result.ReservationResponse;
import kr.hhplus.be.server.reservation.application.port.in.command.ReserveSeatCommand;

public interface ReserveSeatUseCase {
    ReservationResponse reserve(ReserveSeatCommand command);
}