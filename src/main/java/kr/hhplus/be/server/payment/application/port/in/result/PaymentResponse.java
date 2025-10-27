package kr.hhplus.be.server.payment.application.port.in.result;

public record PaymentResponse(
        Long paymentId,
        Long reservationId,
        boolean confirmed
) {}