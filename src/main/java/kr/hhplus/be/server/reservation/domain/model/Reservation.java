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
    private final long amount;

    private final LocalDateTime holdExpiresAt;
    private final LocalDateTime confirmedAt;
    private final LocalDateTime canceledAt;
    private final LocalDateTime expiredAt;

    private final Long version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Reservation(
            Long id,
            Long userId,
            Long concertId,
            Long concertDateId,
            Long seatId,
            ReservationStatus status,
            long amount,
            LocalDateTime holdExpiresAt,
            LocalDateTime confirmedAt,
            LocalDateTime canceledAt,
            LocalDateTime expiredAt,
            Long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
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
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ===== Factory Methods =====
    public static Reservation pending(Long userId, Long concertId, Long concertDateId, Long seatId, long amount, LocalDateTime holdUntil) {
        return new Reservation(
                null,
                userId,
                concertId,
                concertDateId,
                seatId,
                ReservationStatus.PENDING,
                amount,
                holdUntil,
                null, null, null,
                0L,  // version 초기값
                null, null
        );
    }

    public Reservation confirm(LocalDateTime now) {
        if (this.status != ReservationStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태만 확정할 수 있습니다.");
        }
        return new Reservation(
                this.id, this.userId, this.concertId, this.concertDateId, this.seatId,
                ReservationStatus.CONFIRMED,
                this.amount,
                this.holdExpiresAt,
                now,
                this.canceledAt,
                this.expiredAt,
                this.version,
                this.createdAt,
                this.updatedAt
        );
    }

    public Reservation cancel(LocalDateTime now) {
        if (this.status == ReservationStatus.CANCELED) return this;
        return new Reservation(
                this.id, this.userId, this.concertId, this.concertDateId, this.seatId,
                ReservationStatus.CANCELED,
                this.amount,
                this.holdExpiresAt,
                this.confirmedAt,
                now,
                this.expiredAt,
                this.version,
                this.createdAt,
                this.updatedAt
        );
    }

    public Reservation expire(LocalDateTime now) {
        if (this.status != ReservationStatus.PENDING) return this;
        return new Reservation(
                this.id, this.userId, this.concertId, this.concertDateId, this.seatId,
                ReservationStatus.EXPIRED,
                this.amount,
                this.holdExpiresAt,
                this.confirmedAt,
                this.canceledAt,
                now,
                this.version,
                this.createdAt,
                this.updatedAt
        );
    }

    public boolean isHoldActive(LocalDateTime now) {
        return status == ReservationStatus.PENDING && holdExpiresAt != null && holdExpiresAt.isAfter(now);
    }
}