package kr.hhplus.be.server.product.domain.model;

import kr.hhplus.be.server.common.enums.SeatStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ConcertSeat 도메인 모델 (concert_seats 테이블 대응)
 */
@Getter
public class ConcertSeat {

    private final Long id;
    private final Long concertDateId;
    private final int seatNo;
    private final String section;
    private final Long price;
    private final SeatStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ConcertSeat(Long id,
                       Long concertDateId,
                       int seatNo,
                       String section,
                       Long price,
                       SeatStatus status,
                       LocalDateTime createdAt,
                       LocalDateTime updatedAt) {
        this.id = id;
        this.concertDateId = concertDateId;
        this.seatNo = seatNo;
        this.section = section;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean isAvailable() {
        return this.status == SeatStatus.AVAILABLE;
    }

    public boolean isHeld() {
        return this.status == SeatStatus.HELD;
    }

    public boolean isConfirmed() {
        return this.status == SeatStatus.CONFIRMED;
    }

    public ConcertSeat changeStatus(SeatStatus newStatus) {
        return new ConcertSeat(
                this.id,
                this.concertDateId,
                this.seatNo,
                this.section,
                this.price,
                newStatus,
                this.createdAt,
                this.updatedAt
        );
    }
}