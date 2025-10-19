package kr.hhplus.be.server.api.clean.application.port.in.payment;

public record PaymentResponse(
        Long paymentId,
        Long reservationId,
        boolean confirmed
) {}