package kr.hhplus.be.server.payment.domain.model;

import kr.hhplus.be.server.common.enums.PaymentStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Payment {
    private final Long id;
    private final Long reservationId;
    private final Long userId;
    private final long amount;
    private final String provider;
    private final String providerTxnId;
    private final PaymentStatus status;
    private final String idempotencyKey;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Payment(Long id, Long reservationId, Long userId, long amount,
                   String provider, String providerTxnId, PaymentStatus status,
                   String idempotencyKey, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.userId = userId;
        this.amount = amount;
        this.provider = provider;
        this.providerTxnId = providerTxnId;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment succeeded(Long reservationId, Long userId, long amount, String idemKey) {
        return new Payment(null, reservationId, userId, amount, "WALLET", null,
                PaymentStatus.SUCCEEDED, idemKey, null, null);
    }

    public static Payment failed(Long reservationId, Long userId, long amount, String idemKey) {
        return new Payment(null, reservationId, userId, amount, "WALLET", null,
                PaymentStatus.FAILED, idemKey, null, null);
    }

    public static Payment pending(Long reservationId, Long userId, long amount, String idemKey) {
        return new Payment(null, reservationId, userId, amount, "WALLET", null,
                PaymentStatus.PENDING, idemKey, null, null);
    }
}