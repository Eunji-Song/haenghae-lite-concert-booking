package kr.hhplus.be.server.api.layered.entity.wallet;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.payment.PaymentEntity;
import kr.hhplus.be.server.api.layered.entity.user.UsersEntity;
import kr.hhplus.be.server.common.enums.WalletTransactionType;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wallet_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletTransactionEntity extends BaseEntity {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    private UsersEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_payment_id")
    private PaymentEntity relatedPayment;

    @NotNull
    @Column(name = "amount", nullable = false)
    private Long amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private WalletTransactionType type;

    @Size(max = 36)
    @Column(name = "idempotency_key", length = 36)
    private String idempotencyKey;

}