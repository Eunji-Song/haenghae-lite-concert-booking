package kr.hhplus.be.server.payment.application.port.out;

import kr.hhplus.be.server.payment.domain.model.Payment;

import java.util.Optional;

/**
 * 결제 저장소 포트 (영속성 추상화).
 */
public interface PaymentRepository {
    boolean existsByIdempotencyKey(String idempotencyKey);
    Payment save(Payment payment);
    Optional<Payment> findSucceededByReservationId(Long reservationId);

    Optional<Payment> findById(Long paymentId);
}