package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


import kr.hhplus.be.server.api.clean.application.port.out.payment.PaymentRepository;
import kr.hhplus.be.server.api.clean.domain.model.payment.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PaymentRepository의 JPA 구현체
 * 도메인 Repository 인터페이스를 JPA Entity와 연결
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PaymentJpaAdapter implements PaymentRepository {

    private final PaymentEntityRepository entityRepository;
    private final PaymentMapper mapper;

    @Override
    public Payment save(Payment payment) {
        log.debug("결제 저장 - paymentId: {}", payment.getId());

        // 도메인 → 엔티티 변환
        PaymentEntity entity = mapper.toEntity(payment);

        // JPA 저장
        PaymentEntity savedEntity = entityRepository.save(entity);

        // 엔티티 → 도메인 변환
        Payment savedPayment = mapper.toDomain(savedEntity);

        log.debug("결제 저장 완료 - paymentId: {}", savedPayment.getId());
        return savedPayment;
    }

    @Override
    public Optional<Payment> findById(Long id) {
        log.debug("결제 조회 - paymentId: {}", id);

        return entityRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Payment> findByReservationId(Long reservationId) {
        log.debug("예약별 결제 목록 조회 - reservationId: {}", reservationId);

        List<PaymentEntity> entities = entityRepository.findByReservationIdOrderByCreatedAtDesc(reservationId);

        return entities.stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        log.debug("멱등성 키로 결제 조회 - idempotencyKey: {}", idempotencyKey);

        return entityRepository.findByIdempotencyKey(idempotencyKey)
                .map(mapper::toDomain);
    }
}