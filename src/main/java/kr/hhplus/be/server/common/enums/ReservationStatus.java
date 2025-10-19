package kr.hhplus.be.server.common.enums;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELED,
    EXPIRED;

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isConfirmed() {
        return this == CONFIRMED;
    }

    public boolean isCanceled() {
        return this == CANCELED;
    }

    public boolean isExpired() {
        return this == EXPIRED;
    }
}
