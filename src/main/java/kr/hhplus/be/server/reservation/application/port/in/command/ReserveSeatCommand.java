package kr.hhplus.be.server.reservation.application.port.in.command;


public record ReserveSeatCommand(
        String userUuid,
        Long concertId,
        java.time.LocalDate date,
        int seatNo,
        long holdSeconds
) {}