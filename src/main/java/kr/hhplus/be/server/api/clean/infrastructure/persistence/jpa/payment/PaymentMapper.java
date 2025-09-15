package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.payment;

import kr.hhplus.be.server.api.clean.domain.model.payment.Payment;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.reservation.ReservationEntity;
import org.springframework.stereotype.Component;

/**
 * Payment 도메인과 PaymentEntity 간 변환
 */
@Component
public class PaymentMapper {

    /**
     * 도메인 → 엔티티 변환
     */
    public PaymentEntity toEntity(Payment domain) {
        if (domain == null) {
            return null;
        }

        var builder = PaymentEntity.builder()
                .reservation(createReservationReference(domain.getReservationId()))
                .amount(domain.getAmount())
                .status(domain.getStatus())
                .idempotencyKey(domain.getIdempotencyKey())
                .providerTxnId(domain.getProviderTxnId());

        // ID가 있으면 설정 (업데이트 시)
        if (domain.getId() != null) {
            builder.id(domain.getId());
        }

        return builder.build();
    }

    /**
     * 엔티티 → 도메인 변환
     */
    public Payment toDomain(PaymentEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Payment(
                entity.getId(),
                entity.getReservation().getId(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getIdempotencyKey(),
                entity.getProviderTxnId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    // === 연관 엔티티 참조 생성 헬퍼 메서드 ===

    /**
     * Reservation 엔티티 참조 생성 (ID만 설정)
     */
    private ReservationEntity createReservationReference(Long reservationId) {
        if (reservationId == null) {
            return null;
        }

        return ReservationEntity.builder()
                .id(reservationId)
                .build();
    }
}