package kr.hhplus.be.server.api.clean.application.port.in.reservation;


import kr.hhplus.be.server.common.enums.ReservationStatus;

/**
 * 예약 요약 정보 Response (목록용)
 */
public record ReservationSummaryResponse(
        Long reservationId,
        Long concertId,
        String date,
        Long seatNo,
        ReservationStatus status,
        Long paidAmount
) {}