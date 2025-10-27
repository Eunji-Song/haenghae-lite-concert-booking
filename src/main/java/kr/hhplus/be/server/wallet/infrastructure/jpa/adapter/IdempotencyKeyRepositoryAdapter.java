package kr.hhplus.be.server.wallet.infrastructure.jpa.adapter;

import kr.hhplus.be.server.wallet.domain.repository.IdempotencyKeyRepository;
import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.IdempotencyKeyEntity;
import kr.hhplus.be.server.wallet.infrastructure.jpa.repository.IdempotencyKeyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IdempotencyKeyRepositoryAdapter implements IdempotencyKeyRepository {

    private final IdempotencyKeyJpaRepository jpa;

    @Override
    public boolean exists(String requestKey) {
        return jpa.existsByRequestKey(requestKey);
    }

    @Override
    @Transactional
    public void save(String requestKey) {
        if (!jpa.existsByRequestKey(requestKey)) {
            jpa.save(IdempotencyKeyEntity.of(requestKey));
        }
    }
}