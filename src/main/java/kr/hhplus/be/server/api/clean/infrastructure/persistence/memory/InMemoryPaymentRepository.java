package kr.hhplus.be.server.api.clean.infrastructure.persistence.memory;

import kr.hhplus.be.server.api.clean.application.port.out.payment.PaymentRepository;
import kr.hhplus.be.server.api.clean.domain.model.payment.Payment;
import kr.hhplus.be.server.common.enums.PaymentStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PaymentRepository 임시 구현체 (In-Memory)
 * 실제 JPA 구현체가 완성될 때까지 사용
 */
@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<Long, Payment> payments = new ConcurrentHashMap<>();
    private final Map<String, Payment> paymentsByIdempotencyKey = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Payment save(Payment payment) {
        // ID가 없으면 새 ID 생성
        if (payment.getId() == null) {
            Long newId = idGenerator.getAndIncrement();
            // 새 객체 생성 (ID 포함)
            Payment savedPayment = new Payment(
                    newId,
                    payment.getReservationId(),
                    payment.getAmount(),
                    payment.getStatus(),
                    payment.getIdempotencyKey(),
                    payment.getProviderTxnId(),
                    payment.getCreatedAt(),
                    payment.getUpdatedAt()
            );

            payments.put(newId, savedPayment);

            // 멱등성 키로도 인덱싱
            if (savedPayment.getIdempotencyKey() != null) {
                paymentsByIdempotencyKey.put(savedPayment.getIdempotencyKey(), savedPayment);
            }

            return savedPayment;
        } else {
            // 기존 객체 업데이트
            payments.put(payment.getId(), payment);

            // 멱등성 키 인덱스도 업데이트
            if (payment.getIdempotencyKey() != null) {
                paymentsByIdempotencyKey.put(payment.getIdempotencyKey(), payment);
            }

            return payment;
        }
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return Optional.ofNullable(payments.get(id));
    }

    @Override
    public List<Payment> findByReservationId(Long reservationId) {
        return payments.values().stream()
                .filter(payment -> payment.getReservationId().equals(reservationId))
                .toList();
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(paymentsByIdempotencyKey.get(idempotencyKey));
    }

    // 테스트 지원 메서드들

    /**
     * 모든 데이터 초기화 (테스트용)
     */
    public void clear() {
        payments.clear();
        paymentsByIdempotencyKey.clear();
        idGenerator.set(1);
    }

    /**
     * 전체 결제 건수 조회 (테스트용)
     */
    public int count() {
        return payments.size();
    }

    /**
     * 특정 상태의 결제 건수 조회 (테스트용)
     */
    public long countByStatus(PaymentStatus status) {
        return payments.values().stream()
                .filter(payment -> payment.getStatus() == status)
                .count();
    }

    /**
     * 성공한 결제 목록 조회 (테스트용)
     */
    public List<Payment> findSucceededPayments() {
        return payments.values().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCEEDED)
                .toList();
    }
}