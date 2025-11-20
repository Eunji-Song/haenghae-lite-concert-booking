package kr.hhplus.be.server.reservation.application.port.in.result;


public record ReservationDetailResponse(
        Long reservationId,
        Long concertId,
        String date,
        Long seatNo,
        String status,
        Long paidAmount,
        java.time.LocalDateTime holdExpiresAt,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt
) {}