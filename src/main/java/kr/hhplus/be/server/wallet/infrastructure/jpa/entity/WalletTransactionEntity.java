package kr.hhplus.be.server.wallet.infrastructure.jpa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import kr.hhplus.be.server.common.enums.WalletTransactionType;
import kr.hhplus.be.server.common.jpa.CreatedOnlyEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.entity.PaymentEntity;
import lombok.*;
import org.hibernate.annotations.Comment;

@Entity
@Table(
        name = "wallet_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wallet_txn_user_idempo",
                        columnNames = {"user_id", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_wallet_txn_user", columnList = "user_id"),
                @Index(name = "idx_wallet_txn_payment", columnList = "related_payment_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class WalletTransactionEntity extends CreatedOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_wallet_txn_user")
    )
    @Comment("FK: users.id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "related_payment_id",
            foreignKey = @ForeignKey(name = "fk_wallet_txn_payment")
    )
    @Comment("연관 결제 ID (nullable)")
    private PaymentEntity relatedPayment;

    @Min(0)
    @Column(name = "amount", nullable = false)
    @Comment("거래 금액(원)")
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    @Comment("거래 유형(CHARGE, DEBIT, REFUND)")
    private WalletTransactionType type;

    @Column(name = "idempotency_key", length = 36)
    @Comment("멱등 키 (user_id와 복합 유니크)")
    private String idempotencyKey;

    public static WalletTransactionEntity of(UserEntity user,
                                             PaymentEntity payment,
                                             long amount,
                                             WalletTransactionType type,
                                             String idemKey) {
        return WalletTransactionEntity.builder()
                .user(user)
                .relatedPayment(payment)
                .amount(amount)
                .type(type)
                .idempotencyKey(idemKey)
                .build();
    }
}