package kr.hhplus.be.server.api.clean.application.port.in.reservation;

/**
 * 예약 상세 조회 Query
 *
 * Command가 아닌 Query를 사용하는 이유
 * Command 는 상태를 변경할 때 사용하고, Query는 조회를 할 때 사용
 */
public record GetReservationQuery(
        Long reservationId,
        Long userId  // 권한 확인용
) {
    public GetReservationQuery {
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("예약 ID는 필수입니다");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
    }
}