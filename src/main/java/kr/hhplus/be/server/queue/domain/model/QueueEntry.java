package kr.hhplus.be.server.queue.domain.model;

import kr.hhplus.be.server.common.enums.QueueStatus;

/**
 * 대기열 엔트리 (Value Object)
 * 불변 데이터: 대기열 토큰, 사용자, 공연, 순위, 상태
 */
public record QueueEntry(
        String token,
        String userUuid,
        Long concertId,
        long rank,
        QueueStatus status
) {
    public boolean isActive() {
        return status == QueueStatus.ACTIVE;
    }

    public boolean isExpired() {
        return status == QueueStatus.EXPIRED;
    }

    public QueueEntry withStatus(QueueStatus newStatus) {
        return new QueueEntry(token, userUuid, concertId, rank, newStatus);
    }
}