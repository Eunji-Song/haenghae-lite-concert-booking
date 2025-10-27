package kr.hhplus.be.server.common.fixture;

import kr.hhplus.be.server.product.domain.model.Concert;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertDateEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertSeatEntity;

import java.time.LocalDate;

public final class Concerts {
    private Concerts(){}

    // ----- domain -----
    public static Concert domain() {
        return new Concert(1L, "테스트 콘서트", "설명", "가수", "주최", true, null, null);
    }

    // ----- entity -----
    public static ConcertEntity entity() {
        return ConcertEntity.builder()
                .title("테스트 콘서트")
                .description("설명")
                .artistName("가수")
                .organizerName("주최")
                .open(true)
                .build();
    }

    public static ConcertDateEntity dateEntity(ConcertEntity concert, LocalDate date, boolean open) {
        return ConcertDateEntity.builder()
                .concert(concert)
                .eventDate(date)
                .venueName("올림픽홀")
                .open(open)
                .build();
    }

    public static ConcertSeatEntity seatEntity(ConcertDateEntity date, int seatNo, long price) {
        return ConcertSeatEntity.builder()
                .concertDate(date)
                .section("")
                .seatNo(seatNo)
                .price(price)
                .build();
    }
}