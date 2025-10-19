package kr.hhplus.be.server.api.clean.application.port.in.reservation;

/**
 * 내 예약 목록 조회 Query
 */
public record GetMyReservationsQuery(
        Long userId
) {
    public GetMyReservationsQuery {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
    }
}