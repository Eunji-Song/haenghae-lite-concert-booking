package kr.hhplus.be.server.payment.infrastructure.persistence.jpa.adapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.payment.application.port.out.PaymentRepository;
import kr.hhplus.be.server.payment.domain.model.Payment;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.entity.PaymentEntity;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.mapper.PaymentJpaMapper;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.repository.PaymentJpaRepository;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpa;

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return jpa.existsByIdempotencyKey(idempotencyKey);
    }

    @Override
    @Transactional
    public Payment save(Payment payment) {
        PaymentEntity e = PaymentJpaMapper.toEntity(payment);
        e = e.toBuilder()
                .reservation(em.getReference(ReservationEntity.class, payment.getReservationId()))
                .user(em.getReference(UserEntity.class, payment.getUserId()))
                .build();
        return PaymentJpaMapper.toDomain(jpa.save(e));
    }

    @Override
    public Optional<Payment> findSucceededByReservationId(Long reservationId) {
        return jpa.findByReservation_IdAndStatus(
                        reservationId, kr.hhplus.be.server.common.enums.PaymentStatus.SUCCEEDED)
                .map(PaymentJpaMapper::toDomain);
    }
}