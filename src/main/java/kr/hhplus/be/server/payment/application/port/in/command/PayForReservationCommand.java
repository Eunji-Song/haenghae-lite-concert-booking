package kr.hhplus.be.server.payment.application.port.in.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

public record PayForReservationCommand(
        String userUuid,
        Long reservationId,
        Long amount,
        String queueToken,
        String idempotencyKey
) {
}