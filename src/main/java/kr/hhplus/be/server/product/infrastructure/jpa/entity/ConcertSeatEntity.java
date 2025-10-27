package kr.hhplus.be.server.product.infrastructure.jpa.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.enums.SeatStatus;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "concert_seats",
        uniqueConstraints = @UniqueConstraint(name = "uk_concert_seat", columnNames = {"concert_date_id", "section", "seat_no"}),
        indexes = @Index(name = "idx_concert_seats_price", columnList = "price"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@SQLDelete(sql = "UPDATE concert_seats SET deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ConcertSeatEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concert_date_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_concert_seats_date"))
    @Comment("FK: concert_dates.id")
    private ConcertDateEntity concertDate;

    @Column(name = "seat_no", nullable = false)
    @Comment("좌석 번호")
    private int seatNo;

    @Column(name = "section", nullable = false, length = 50)
    @Comment("구역/층/블록(공백=없음)")
    private String section;

    @Column(name = "price", nullable = false)
    @Comment("좌석 가격(원)")
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Comment("좌석 상태(AVAILABLE/HELD/CONFIRMED)")
    private SeatStatus status = SeatStatus.AVAILABLE;

    /** 좌석 상태 변경 (예약 확정 / 임시 배정 / 해제) */
    public void changeStatus(SeatStatus newStatus) {
        this.status = newStatus;
    }
}