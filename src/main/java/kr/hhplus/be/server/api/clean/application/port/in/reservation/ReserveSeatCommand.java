package kr.hhplus.be.server.api.clean.application.port.in.reservation;

/**
 * 좌석 예약 요청
 */
public record ReserveSeatCommand(
        Long userId,
        Long concertId,
        String date, // "2025-09-10"
        Long seatNo
) {
    public ReserveSeatCommand {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
        if (concertId == null || concertId <= 0) {
            throw new IllegalArgumentException("콘서트 ID는 필수입니다");
        }
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("날짜는 필수입니다");
        }
        if (seatNo == null || seatNo <= 0) {
            throw new IllegalArgumentException("좌석 번호는 필수입니다");
        }
    }
}