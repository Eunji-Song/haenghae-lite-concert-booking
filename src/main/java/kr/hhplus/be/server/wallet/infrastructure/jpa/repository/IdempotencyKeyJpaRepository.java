package kr.hhplus.be.server.wallet.infrastructure.jpa.repository;

import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyEntity, Long> {
    boolean existsByRequestKey(String requestKey);
}