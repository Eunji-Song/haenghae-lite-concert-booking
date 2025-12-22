package kr.hhplus.be.server.reservation.application.port.out;

import kr.hhplus.be.server.common.event.ReservationConfirmedMessage;

public interface ReservationEventOutPort {
    void publishReservationConfirmed(ReservationConfirmedMessage message);
}