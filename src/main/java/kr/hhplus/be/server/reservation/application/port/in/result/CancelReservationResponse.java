package kr.hhplus.be.server.reservation.application.port.in.result;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CancelReservationResponse {
    private final boolean canceled;
}