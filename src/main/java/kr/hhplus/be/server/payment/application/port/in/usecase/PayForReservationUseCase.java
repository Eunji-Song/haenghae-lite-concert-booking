package kr.hhplus.be.server.payment.application.port.in.usecase;

import kr.hhplus.be.server.payment.application.port.in.command.PayForReservationCommand;
import kr.hhplus.be.server.payment.application.port.in.result.PaymentResponse;

public interface PayForReservationUseCase {
    PaymentResponse pay(PayForReservationCommand command);
}