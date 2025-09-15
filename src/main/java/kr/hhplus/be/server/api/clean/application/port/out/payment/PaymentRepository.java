package kr.hhplus.be.server.api.clean.application.port.out.payment;

import kr.hhplus.be.server.api.clean.domain.model.payment.Payment;

import java.util.List;
import java.util.Optional;

/**
 * 결제 Repository 포트
 * Application Layer에서 정의하고 Infrastructure Layer에서 구현
 */
public interface PaymentRepository {

    /**
     * 결제 저장
     */
    Payment save(Payment payment);

    /**
     * ID로 결제 조회
     */
    Optional<Payment> findById(Long id);

    /**
     * 예약별 결제 목록 조회
     */
    List<Payment> findByReservationId(Long reservationId);

    /**
     * 멱등 키로 결제 조회 (중복 결제 방지)
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}