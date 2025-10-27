package kr.hhplus.be.server.queue.api.dto;

import kr.hhplus.be.server.common.enums.QueueStatus;

public record QueueTokenResponse(String queueToken, QueueStatus status, Long rank, Long etaSeconds) {
    public static QueueTokenResponse issued(String token, Long rank, Long eta) {
        return new QueueTokenResponse(token, QueueStatus.ISSUED, rank, eta);
    }

    public static QueueTokenResponse active(String token) {
        return new QueueTokenResponse(token, QueueStatus.ACTIVE, null, null);
    }

    public static QueueTokenResponse expired(String token) {
        return new QueueTokenResponse(token, QueueStatus.EXPIRED, null, null);
    }
}