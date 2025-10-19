package kr.hhplus.be.server.common.enums;

public enum SeatStatus {
    AVAILABLE("예약 가능"),
    HELD("임시 배정"),
    CONFIRMED("예약 확정");

    private final String description;

    SeatStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}