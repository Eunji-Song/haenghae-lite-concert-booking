package kr.hhplus.be.server.api.clean.domain.model.payment;

import kr.hhplus.be.server.common.enums.PaymentStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 순수 도메인 모델: 결제
 * - JPA 어노테이션/엔티티 참조 없음
 * - 핵심 결제 비즈니스 로직 포함
 */
@Getter
public class Payment {
    private Long id;
    private Long reservationId;
    private Long amount;
    private PaymentStatus status;
    private String idempotencyKey;
    private String providerTxnId;  // 외부 결제 시스템 거래 ID
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 새 결제 생성용 생성자 (ID 없음)
    private Payment(Long reservationId, Long amount, String idempotencyKey) {
        this.id = null; // 새 생성시 ID는 null
        this.reservationId = reservationId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
        this.providerTxnId = generateProviderTxnId();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = null;
    }

    public Payment(Long id, Long reservationId, Long amount, PaymentStatus status, String idempotencyKey) {
        this.id = id;
        this.reservationId = reservationId;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
    }


    public Payment(Long id, Long reservationId, Long amount, PaymentStatus status, String idempotencyKey, String providerTxnId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.providerTxnId = providerTxnId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // 팩토리 메서드
    public static Payment createPendingPayment(Long reservationId, Long amount, String idempotencyKey) {
        validateCreatePaymentParams(reservationId, amount, idempotencyKey);
        return new Payment(reservationId, amount, idempotencyKey);
    }

    // 멱등성 키가 없는 경우 자동 생성
    public static Payment createPendingPayment(Long reservationId, Long amount) {
        String autoIdempotencyKey = generateIdempotencyKey(reservationId);
        return createPendingPayment(reservationId, amount, autoIdempotencyKey);
    }

    // 비즈니스 로직 메서드들

    /**
     * 결제 성공 처리
     */
    public void succeed() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 성공 처리할 수 있습니다");
        }

        this.status = PaymentStatus.SUCCEEDED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 결제 실패 처리
     */
    public void fail() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 실패 처리할 수 있습니다");
        }

        this.status = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 결제 완료 여부 확인
     */
    public boolean isCompleted() {
        return status == PaymentStatus.SUCCEEDED || status == PaymentStatus.FAILED;
    }

    /**
     * 결제 성공 여부 확인
     */
    public boolean isSucceeded() {
        return status == PaymentStatus.SUCCEEDED;
    }

    /**
     * 결제 금액 검증
     */
    public void validateAmount(Long expectedAmount) {
        if (!this.amount.equals(expectedAmount)) {
            throw new IllegalArgumentException(
                    String.format("결제 금액이 일치하지 않습니다. 예상: %d원, 실제: %d원", expectedAmount, this.amount)
            );
        }
    }

    /**
     * 멱등성 키 검증
     */
    public boolean hasIdempotencyKey(String idempotencyKey) {
        return this.idempotencyKey != null && this.idempotencyKey.equals(idempotencyKey);
    }

    // private 헬퍼 메서드들

    private static void validateCreatePaymentParams(Long reservationId, Long amount, String idempotencyKey) {
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("예약 ID는 필수입니다");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다");
        }
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("멱등성 키는 필수입니다");
        }
    }

    private static String generateIdempotencyKey(Long reservationId) {
        return String.format("payment_%d_%s", reservationId, UUID.randomUUID().toString());
    }

    private String generateProviderTxnId() {
        // 가상 결제 시스템용 거래 ID 생성
        return String.format("virtual_txn_%s_%d",
                UUID.randomUUID().toString().substring(0, 8),
                System.currentTimeMillis());
    }
}