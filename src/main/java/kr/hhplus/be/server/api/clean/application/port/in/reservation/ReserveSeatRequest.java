package kr.hhplus.be.server.api.clean.application.port.in.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReserveSeatRequest(
        @NotNull Long concertId,
        @NotBlank String date,
        @NotNull @Min(1) Long seatNo
) {}