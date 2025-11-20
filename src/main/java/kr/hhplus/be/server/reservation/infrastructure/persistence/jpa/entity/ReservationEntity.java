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
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_resv_seat_active", columnNames = {"seat_id", "is_active"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@SQLDelete(sql = "UPDATE reservations SET deleted_at = CURRENT_TIMESTAMP(6), is_active = 0 WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ReservationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    private Long id;

    // === FK primitive fields ===
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "concert_id", nullable = false)
    private Long concertId;
    @Column(name = "concert_date_id", nullable = false)
    private Long concertDateId;
    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    // === 읽기 전용 연관 ===
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ReservationStatus status;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "is_active", nullable = false)
    @Comment("좌석 점유 활성 플래그(홀드/확정=1, 취소/만료=0)")
    private boolean isActive;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public void holdUntil(LocalDateTime expiresAt, long amount) {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("예약이 PENDING 상태일 때만 홀드 설정 가능");
        }
        this.amount = amount;
        this.holdExpiresAt = expiresAt;
        this.isActive = true;
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
        this.isActive = true;
    }

    public void cancel(LocalDateTime now) {
        if (status == ReservationStatus.CANCELED) return;
        this.status = ReservationStatus.CANCELED;
        this.canceledAt = now;
        this.isActive = false;
    }

    public void expire(LocalDateTime now) {
        if (status != ReservationStatus.PENDING) return;
        this.status = ReservationStatus.EXPIRED;
        this.expiredAt = now;
        this.isActive = false;
    }
}