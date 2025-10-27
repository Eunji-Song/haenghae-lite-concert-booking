package kr.hhplus.be.server.common.fixture;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertDateEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertSeatEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;

import java.time.LocalDateTime;

public final class Reservations {
    private Reservations(){}

    // ----- domain -----
    public static Reservation pendingDomain(Long userId, Long concertId, Long dateId, Long seatId, long amt, LocalDateTime holdUntil) {
        return Reservation.pending(userId, concertId, dateId, seatId, amt, holdUntil);
    }

    // ----- entity -----
    public static ReservationEntity pendingEntity(UserEntity user, ConcertEntity concert,
                                                  ConcertDateEntity date, ConcertSeatEntity seat,
                                                  long amount, LocalDateTime holdUntil) {

        return ReservationEntity.builder()
                .user(user)
                .concert(concert)
                .concertDate(date)
                .seat(seat)
                .status(ReservationStatus.PENDING)
                .amount(amount)
                .holdExpiresAt(holdUntil)
                .build();
    }
}