package kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertDateEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertSeatEntity;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reservations",
        indexes = {
                @Index(name = "idx_resv_user", columnList = "user_id"),
                @Index(name = "idx_resv_concert", columnList = "concert_id"),
                @Index(name = "idx_resv_date", columnList = "concert_date_id"),
                @Index(name = "idx_resv_status", columnList = "status"),
                @Index(name = "idx_resv_hold_exp", columnList = "hold_expires_at"),
                @Index(name = "idx_resv_confirmed_at", columnList = "confirmed_at"),
                @Index(name = "idx_resv_canceled_at", columnList = "canceled_at"),
                @Index(name = "idx_resv_expired_at", columnList = "expired_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@SQLDelete(sql = "UPDATE reservations SET deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ReservationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    private Long id;

    // === FK primitive fields (실제 INSERT/UPDATE는 이 값들이 들어감) ===
    @Column(name = "user_id", nullable = false)
    @Comment("FK: users.id")
    private Long userId;

    @Column(name = "concert_id", nullable = false)
    @Comment("FK: concerts.id")
    private Long concertId;

    @Column(name = "concert_date_id", nullable = false)
    @Comment("FK: concert_dates.id")
    private Long concertDateId;

    @Column(name = "seat_id", nullable = false)
    @Comment("FK: concert_seats.id")
    private Long seatId;

    // === 읽기 전용 연관관계 (조회 편의용) ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_resv_user"))
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_resv_concert"))
    private ConcertEntity concert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_date_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_resv_date"))
    private ConcertDateEntity concertDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_resv_seat"))
    private ConcertSeatEntity seat;

    // === 상태/타임스탬프 ===
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Comment("예약 상태(PENDING, CONFIRMED, CANCELED, EXPIRED)")
    private ReservationStatus status;

    @Column(name = "amount", nullable = false)
    @Comment("결제 예정/실제 금액(원)")
    private long amount;

    @Column(name = "hold_expires_at")
    @Comment("홀드 만료 시각(UTC)")
    private LocalDateTime holdExpiresAt;

    @Column(name = "confirmed_at")
    @Comment("확정 시각(UTC)")
    private LocalDateTime confirmedAt;

    @Column(name = "canceled_at")
    @Comment("취소 시각(UTC)")
    private LocalDateTime canceledAt;

    @Column(name = "expired_at")
    @Comment("만료 처리 시각(UTC)")
    private LocalDateTime expiredAt;

    @Version
    @Column(name = "version", nullable = false)
    @Comment("낙관적 락 버전")
    private Long version;

    public void holdUntil(LocalDateTime expiresAt, long amount) {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("예약이 PENDING 상태일 때만 홀드 설정 가능");
        }
        this.amount = amount;
        this.holdExpiresAt = expiresAt;
    }

    public boolean isHoldActive(LocalDateTime now) {
        return status == ReservationStatus.PENDING && holdExpiresAt != null && holdExpiresAt.isAfter(now);
    }

    public void confirm(LocalDateTime now) {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("PENDING만 확정 가능");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = now;
    }

    public void cancel(LocalDateTime now) {
        if (status == ReservationStatus.CANCELED) return;
        this.status = ReservationStatus.CANCELED;
        this.canceledAt = now;
    }

    public void expire(LocalDateTime now) {
        if (status != ReservationStatus.PENDING) return;
        this.status = ReservationStatus.EXPIRED;
        this.expiredAt = now;
    }
}