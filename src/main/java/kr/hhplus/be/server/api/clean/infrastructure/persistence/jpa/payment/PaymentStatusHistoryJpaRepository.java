package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentStatusHistoryJpaRepository extends JpaRepository<PaymentStatusHistoryEntity, Long> {
}