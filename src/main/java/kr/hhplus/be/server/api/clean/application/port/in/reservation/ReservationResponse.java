package kr.hhplus.be.server.api.clean.application.port.in.reservation;

import java.time.LocalDateTime;

/**
 * 좌석 예약 결과 Result
 */
public record ReservationResponse(
        Long reservationId,
        LocalDateTime holdExpiresAt
) {}