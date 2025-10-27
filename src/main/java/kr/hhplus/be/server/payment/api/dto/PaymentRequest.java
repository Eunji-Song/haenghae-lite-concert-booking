package kr.hhplus.be.server.payment.api.dto;


public record PaymentRequest(
        Long reservationId,
        Long amount
) {}