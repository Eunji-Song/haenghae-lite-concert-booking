package kr.hhplus.be.server.payment.infrastructure.persistence.memory;

public record QueueEntry(
        String token,
        String userUuid,
        Long concertId,
        Long rank
) {}