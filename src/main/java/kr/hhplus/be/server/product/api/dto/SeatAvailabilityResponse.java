package kr.hhplus.be.server.product.api.dto;


import kr.hhplus.be.server.common.enums.SeatStatus;

public record SeatAvailabilityResponse(
        Long seatId,
        int seatNo,
        Long price,
        SeatStatus status
) {}