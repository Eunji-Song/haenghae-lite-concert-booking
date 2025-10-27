package kr.hhplus.be.server.product.infrastructure.jpa.mapper;

import kr.hhplus.be.server.common.enums.SeatStatus;
import kr.hhplus.be.server.product.domain.model.Concert;
import kr.hhplus.be.server.product.domain.model.ConcertDate;
import kr.hhplus.be.server.product.domain.model.ConcertSeat;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertDateEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertSeatEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductJpaMapper {

    /* ===============================
     *  Concert
     * =============================== */
    public static Concert toDomain(ConcertEntity e) {
        if (e == null) return null;
        return new Concert(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getArtistName(),
                e.getOrganizerName(),
                e.isOpen(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public static ConcertEntity toEntity(Concert d) {
        if (d == null) return null;
        return ConcertEntity.builder()
                .id(d.getId())
                .title(d.getTitle())
                .description(d.getDescription())
                .artistName(d.getArtistName())
                .organizerName(d.getOrganizerName())
                .open(d.isOpen())
                .build();
    }

    /* ===============================
     *  ConcertDate
     * =============================== */
    public static ConcertDate toDomain(ConcertDateEntity e) {
        if (e == null) return null;
        return new ConcertDate(
                e.getId(),
                e.getConcert().getId(),
                e.getEventDate(),
                e.getVenueName(),
                e.isOpen(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public static ConcertDateEntity toEntity(ConcertDate d, ConcertEntity concertRef) {
        if (d == null) return null;
        return ConcertDateEntity.builder()
                .id(d.getId())
                .concert(concertRef)
                .eventDate(d.getEventDate())
                .venueName(d.getVenueName())
                .open(d.isOpen())
                .build();
    }

    /* ===============================
     *  ConcertSeat
     * =============================== */
    public static ConcertSeat toDomain(ConcertSeatEntity e) {
        if (e == null) return null;
        return new ConcertSeat(
                e.getId(),
                e.getConcertDate().getId(),
                e.getSeatNo(),
                e.getSection(),
                e.getPrice(),
                e.getStatus() != null ? e.getStatus() : SeatStatus.AVAILABLE,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public static ConcertSeatEntity toEntity(ConcertSeat d, ConcertDateEntity dateRef) {
        if (d == null) return null;
        return ConcertSeatEntity.builder()
                .id(d.getId())
                .concertDate(dateRef)
                .seatNo(d.getSeatNo())
                .section(d.getSection())
                .price(d.getPrice())
                .status(d.getStatus() != null ? d.getStatus() : SeatStatus.AVAILABLE)
                .build();
    }
}