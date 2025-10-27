package kr.hhplus.be.server.reservation.domain.model;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Reservation {
    private final Long id;
    private final Long userId;
    private final Long concertId;
    private final Long concertDateId;
    private final Long seatId;
    private final ReservationStatus status;
    private final Long amount;
    private final LocalDateTime holdExpiresAt;
    private final LocalDateTime confirmedAt;
    private final LocalDateTime canceledAt;
    private final LocalDateTime expiredAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Reservation(Long id, Long userId, Long concertId, Long concertDateId, Long seatId,
                       ReservationStatus status, Long amount, LocalDateTime holdExpiresAt,
                       LocalDateTime confirmedAt, LocalDateTime canceledAt, LocalDateTime expiredAt,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.concertId = concertId;
        this.concertDateId = concertDateId;
        this.seatId = seatId;
        this.status = status;
        this.amount = amount;
        this.holdExpiresAt = holdExpiresAt;
        this.confirmedAt = confirmedAt;
        this.canceledAt = canceledAt;
        this.expiredAt = expiredAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Reservation pending(Long userId, Long concertId, Long concertDateId, Long seatId, Long amount, LocalDateTime holdUntil) {
        return new Reservation(null, userId, concertId, concertDateId, seatId,
                ReservationStatus.PENDING, amount, holdUntil,
                null, null, null, null, null);
    }
}