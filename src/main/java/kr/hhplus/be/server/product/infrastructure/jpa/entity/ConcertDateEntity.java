package kr.hhplus.be.server.product.infrastructure.jpa.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "concert_dates",
        uniqueConstraints = @UniqueConstraint(name = "uk_concert_date", columnNames = {"concert_id", "event_date"}),
        indexes = @Index(name = "idx_concert_dates_open", columnList = "is_open"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@SQLDelete(sql = "UPDATE concert_dates SET deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ConcertDateEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concert_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_concert_dates_concert"))
    @Comment("FK: concerts.id")
    private ConcertEntity concert;

    @Column(name = "event_date", nullable = false)
    @Comment("공연 날짜(YYYY-MM-DD)")
    private LocalDate eventDate;

    @Column(name = "venue_name", nullable = false, length = 200)
    @Comment("공연장 이름")
    private String venueName;

    @Column(name = "is_open", nullable = false)
    @Comment("예매 오픈 여부")
    private boolean open;

    public void markOpen(boolean open) {
        this.open = open;
    }
}