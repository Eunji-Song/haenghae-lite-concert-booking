package kr.hhplus.be.server.reservation.application.port.in.result;

import java.time.LocalDateTime;

/**
 * 좌석 예약 결과 Result
 */

public record ReservationResponse(
        Long reservationId,
        LocalDateTime holdExpiresAt
) {}