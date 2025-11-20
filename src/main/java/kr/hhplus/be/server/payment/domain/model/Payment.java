package kr.hhplus.be.server.payment.domain.model;

import kr.hhplus.be.server.common.enums.PaymentStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public class Payment {

    public static final String PROVIDER_WALLET = "WALLET";

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

    public Payment(Long id,
                   Long reservationId,
                   Long userId,
                   long amount,
                   String provider,
                   String providerTxnId,
                   PaymentStatus status,
                   String idempotencyKey,
                   LocalDateTime createdAt,
                   LocalDateTime updatedAt) {

        // ===== 도메인 불변 조건 방어 =====
        if (reservationId == null) throw new IllegalArgumentException("reservationId must not be null");
        if (userId == null) throw new IllegalArgumentException("userId must not be null");
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        if (status == null) throw new IllegalArgumentException("status must not be null");

        this.id = id;
        this.reservationId = reservationId;
        this.userId = userId;
        this.amount = amount;
        this.provider = (provider == null || provider.isBlank()) ? PROVIDER_WALLET : provider;
        this.providerTxnId = providerTxnId;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ===== 팩토리 메서드 =====
    public static Payment succeeded(Long reservationId, Long userId, long amount, String idemKey) {
        return new Payment(
                null, reservationId, userId, amount,
                PROVIDER_WALLET, null,
                PaymentStatus.SUCCEEDED, idemKey,
                null, null
        );
    }

    public static Payment failed(Long reservationId, Long userId, long amount, String idemKey) {
        return new Payment(
                null, reservationId, userId, amount,
                PROVIDER_WALLET, null,
                PaymentStatus.FAILED, idemKey,
                null, null
        );
    }

    public static Payment pending(Long reservationId, Long userId, long amount, String idemKey) {
        return new Payment(
                null, reservationId, userId, amount,
                PROVIDER_WALLET, null,
                PaymentStatus.PENDING, idemKey,
                null, null
        );
    }

    // ===== 편의 메서드 =====
    public boolean isSuccess() {
        return this.status == PaymentStatus.SUCCEEDED;
    }

    public Payment withProviderTxnId(String providerTxnId) {
        return new Payment(
                this.id, this.reservationId, this.userId, this.amount,
                this.provider, providerTxnId,
                this.status, this.idempotencyKey,
                this.createdAt, this.updatedAt
        );
    }

    public Payment withId(Long id) {
        return new Payment(
                id, this.reservationId, this.userId, this.amount,
                this.provider, this.providerTxnId,
                this.status, this.idempotencyKey,
                this.createdAt, this.updatedAt
        );
    }

    // (필요시 equals/hashCode 추가)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment)) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}