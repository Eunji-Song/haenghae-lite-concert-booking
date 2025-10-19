package kr.hhplus.be.server.api.clean.application.port.in.payment;

/**
 * 결제 및 예약 완료 요청
 */
public record PayForReservationCommand(
        Long reservationId,
        Long amount,
        String idempotencyKey
) {
    public PayForReservationCommand {
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("예약 ID는 필수입니다");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다");
        }
    }
}