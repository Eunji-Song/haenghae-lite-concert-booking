package kr.hhplus.be.server.api.clean.application.port.in.reservation;

/**
 * 예약 취소 요청
 */
public record CancelReservationCommand(
        Long reservationId,
        Long userId  // 권한 확인용
) {
    public CancelReservationCommand {
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("예약 ID는 필수입니다");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
    }
}