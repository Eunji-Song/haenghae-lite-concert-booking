package kr.hhplus.be.server.common.enums;

/**
 * 대기열 상태
 * - ISSUED: 대기열 토큰 발급됨 (대기 중)
 * - ACTIVE: 대기열에서 통과하여 사용 가능
 * - EXPIRED: 대기열 토큰 만료됨
 */
public enum QueueStatus {
    ISSUED,
    ACTIVE,
    EXPIRED;

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isExpired() {
        return this == EXPIRED;
    }
}