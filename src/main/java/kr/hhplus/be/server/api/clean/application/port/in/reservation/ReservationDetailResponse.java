package kr.hhplus.be.server.api.clean.application.port.in.reservation;

import kr.hhplus.be.server.common.enums.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationDetailResponse(
        Long reservationId,
        Long concertId,
        String date,
        Long seatNo,
        ReservationStatus status,
        Long paidAmount,
        LocalDateTime holdExpiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
