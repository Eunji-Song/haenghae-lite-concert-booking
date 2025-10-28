package kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.mapper;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public final class ReservationJpaMapper {
    private ReservationJpaMapper(){}

    public static ReservationEntity toEntity(Reservation d) {
        if (d == null) return null;
        return ReservationEntity.builder()
                .id(d.getId())
                .userId(d.getUserId())
                .concertId(d.getConcertId())
                .concertDateId(d.getConcertDateId())
                .seatId(d.getSeatId())
                .status(d.getStatus())
                .amount(d.getAmount())
                .holdExpiresAt(d.getHoldExpiresAt())
                .confirmedAt(d.getConfirmedAt())
                .canceledAt(d.getCanceledAt())
                .expiredAt(d.getExpiredAt())
                .version(d.getVersion() == null ? 0L : d.getVersion())
                .build();
    }

    public static Reservation toDomain(ReservationEntity e) {
        if (e == null) return null;
        return new Reservation(
                e.getId(),
                e.getUserId(),
                e.getConcertId(),
                e.getConcertDateId(),
                e.getSeatId(),
                e.getStatus(),
                e.getAmount(),
                e.getHoldExpiresAt(),
                e.getConfirmedAt(),
                e.getCanceledAt(),
                e.getExpiredAt(),
                e.getVersion(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}