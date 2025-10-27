package kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.mapper;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReservationJpaMapper {

    public static Reservation toDomain(ReservationEntity e) {
        if (e == null) return null;
        return new Reservation(
                e.getId(),
                e.getUser().getId(),
                e.getConcert().getId(),
                e.getConcertDate().getId(),
                e.getSeat().getId(),
                e.getStatus(),
                e.getAmount(),
                e.getHoldExpiresAt(),
                e.getConfirmedAt(),
                e.getCanceledAt(),
                e.getExpiredAt(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public static ReservationEntity toEntity(Reservation d) {
        if (d == null) return null;
        return ReservationEntity.builder()
                .id(d.getId())
                .status(d.getStatus() != null ? d.getStatus() : ReservationStatus.PENDING)
                .amount(d.getAmount() != null ? d.getAmount() : 0L)
                .holdExpiresAt(d.getHoldExpiresAt())
                .confirmedAt(d.getConfirmedAt())
                .canceledAt(d.getCanceledAt())
                .expiredAt(d.getExpiredAt())
                .build();
    }
}