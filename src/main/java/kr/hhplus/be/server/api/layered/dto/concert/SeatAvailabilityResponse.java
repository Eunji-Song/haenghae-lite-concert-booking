package kr.hhplus.be.server.api.layered.dto.concert;

public record SeatAvailabilityResponse(Long seatId, Long seatNo, Long price, String state) {
    public static SeatAvailabilityResponse available(Long seatId, Long seatNo, Long price) {
        return new SeatAvailabilityResponse(seatId, seatNo, price, "AVAILABLE");
    }
    public static SeatAvailabilityResponse unavailable(Long seatId, Long seatNo, Long price) {
        return new SeatAvailabilityResponse(seatId, seatNo, price, "UNAVAILABLE");
    }
}