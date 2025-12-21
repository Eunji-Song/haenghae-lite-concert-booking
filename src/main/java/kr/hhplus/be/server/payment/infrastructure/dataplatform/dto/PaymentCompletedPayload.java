package kr.hhplus.be.server.payment.infrastructure.dataplatform.dto;

import java.time.Instant;

public record PaymentCompletedPayload(
        Long paymentId,
        Long reservationId,
        String userUuid,
        Long concertId,
        Long amount,
        String idempotencyKey,
        Instant occurredAt
) {}