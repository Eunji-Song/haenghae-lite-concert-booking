package kr.hhplus.be.server.product.domain.model;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ConcertDate 도메인 모델 (concert_dates 테이블 대응)
 */
@Getter
public class ConcertDate {

    private final Long id;
    private final Long concertId;
    private final LocalDate eventDate;
    private final String venueName;
    private final boolean open;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ConcertDate(Long id,
                       Long concertId,
                       LocalDate eventDate,
                       String venueName,
                       boolean open,
                       LocalDateTime createdAt,
                       LocalDateTime updatedAt) {
        this.id = id;
        this.concertId = concertId;
        this.eventDate = eventDate;
        this.venueName = venueName;
        this.open = open;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}