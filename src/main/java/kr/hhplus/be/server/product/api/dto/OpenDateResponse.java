package kr.hhplus.be.server.product.api.dto;


import java.time.LocalDate;

public record OpenDateResponse(
        Long concertDateId,
        Long concertId,
        LocalDate date,
        boolean open
) {}