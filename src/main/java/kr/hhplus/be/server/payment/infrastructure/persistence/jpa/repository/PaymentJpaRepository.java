package kr.hhplus.be.server.payment.infrastructure.persistence.jpa.repository;

import kr.hhplus.be.server.common.enums.PaymentStatus;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
    Optional<PaymentEntity> findByReservation_IdAndStatus(Long reservationId, PaymentStatus status);
}