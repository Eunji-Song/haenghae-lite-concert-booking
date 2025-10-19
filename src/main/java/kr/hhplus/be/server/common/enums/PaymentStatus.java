package kr.hhplus.be.server.common.enums;

public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED;

    public boolean isFinal() {
        return this == SUCCEEDED || this == FAILED;
    }
}
