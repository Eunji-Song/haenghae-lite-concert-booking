package kr.hhplus.be.server.payment.application.port.in.command;

public record PayForReservationCommand(
        String userUuid,
        Long reservationId,
        Long amount,
        String queueToken,
        String idempotencyKey
) {
}