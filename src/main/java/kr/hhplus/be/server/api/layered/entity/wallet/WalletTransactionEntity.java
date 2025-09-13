package kr.hhplus.be.server.api.layered.entity.wallet;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.payment.PaymentEntity;
import kr.hhplus.be.server.api.layered.entity.user.UserEntity;
import kr.hhplus.be.server.common.enums.WalletTransactionType;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.*;

@Entity
@Table(name = "wallet_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WalletTransactionEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    private UserEntity user;

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


    public static WalletTransactionEntity createCharge(WalletAccountEntity account, long amount, String idemKey) {
        WalletTransactionEntity txn = new WalletTransactionEntity();
        txn.user = account.getUser();
        txn.amount = amount;
        txn.type = WalletTransactionType.CHARGE;
        txn.idempotencyKey = idemKey;
        return txn;
    }

}