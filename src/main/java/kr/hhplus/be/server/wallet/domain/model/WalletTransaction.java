package kr.hhplus.be.server.wallet.domain.model;

import kr.hhplus.be.server.common.enums.WalletTransactionType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class WalletTransaction {
    private final Long id;
    private final Long userId;
    private final Long relatedPaymentId;
    private final long amount;
    private final WalletTransactionType type;
    private final String idempotencyKey;
    private final LocalDateTime createdAt;

    public WalletTransaction(Long id, Long userId, Long relatedPaymentId, long amount,
                             WalletTransactionType type, String idempotencyKey, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.relatedPaymentId = relatedPaymentId;
        this.amount = amount;
        this.type = type;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public static WalletTransaction charge(Long userId, long amount, String idem) {
        return new WalletTransaction(null, userId, null, amount, WalletTransactionType.CHARGE, idem, null);
    }

    public static WalletTransaction debit(Long userId, Long paymentId, long amount, String idem) {
        return new WalletTransaction(null, userId, paymentId, amount, WalletTransactionType.DEBIT, idem, null);
    }

    public static WalletTransaction refund(Long userId, Long paymentId, long amount, String idem) {
        return new WalletTransaction(null, userId, paymentId, amount, WalletTransactionType.REFUND, idem, null);
    }
}