package kr.hhplus.be.server.payment.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.enums.PaymentStatus;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payments_idempo", columnNames = "idempotency_key"),
                @UniqueConstraint(name = "uk_payments_one_success_per_resv", columnNames = {"reservation_id", "is_success"})
        },
        indexes = {
                @Index(name = "idx_payments_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@SQLDelete(sql = "UPDATE payments SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class PaymentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reservation_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_reservation")
    )
    @Comment("FK: reservations.id")
    private ReservationEntity reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_user")
    )
    @Comment("FK: users.id")
    private UserEntity user;

    @Column(name = "amount", nullable = false)
    @Comment("결제 금액(원)")
    private long amount;

    @Column(name = "provider", length = 50)
    @Comment("결제 수단/PG사")
    private String provider;

    @Column(name = "provider_txn_id", length = 100)
    @Comment("외부 거래 ID")
    private String providerTxnId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Comment("결제 상태(PENDING, SUCCEEDED, FAILED)")
    private PaymentStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 36)
    @Comment("멱등 키(UUID)")
    private String idempotencyKey;

    @Column(name = "is_success", insertable = false, updatable = false)
    @Comment("성공 결제 플래그(GENERATED COLUMN)")
    private Boolean isSuccess;
}