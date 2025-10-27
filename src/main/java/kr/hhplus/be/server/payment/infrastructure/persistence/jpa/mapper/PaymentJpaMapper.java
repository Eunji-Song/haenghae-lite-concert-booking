package kr.hhplus.be.server.payment.infrastructure.persistence.jpa.mapper;

import kr.hhplus.be.server.payment.domain.model.Payment;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.entity.PaymentEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaymentJpaMapper {

    public static Payment toDomain(PaymentEntity e) {
        if (e == null) return null;
        return new Payment(
                e.getId(),
                e.getReservation().getId(),
                e.getUser().getId(),
                e.getAmount(),
                e.getProvider(),
                e.getProviderTxnId(),
                e.getStatus(),
                e.getIdempotencyKey(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public static PaymentEntity toEntity(Payment d) {
        if (d == null) return null;
        return PaymentEntity.builder()
                .id(d.getId())
                .amount(d.getAmount())
                .provider(d.getProvider())
                .providerTxnId(d.getProviderTxnId())
                .status(d.getStatus())
                .idempotencyKey(d.getIdempotencyKey())
                .build();
    }
}