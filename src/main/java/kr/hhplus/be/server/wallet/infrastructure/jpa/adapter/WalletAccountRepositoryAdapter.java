package kr.hhplus.be.server.wallet.infrastructure.jpa.adapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.wallet.domain.model.WalletAccount;
import kr.hhplus.be.server.wallet.domain.repository.WalletAccountRepository;
import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.WalletAccountEntity;
import kr.hhplus.be.server.wallet.infrastructure.jpa.mapper.WalletJpaMapper;
import kr.hhplus.be.server.wallet.infrastructure.jpa.repository.WalletAccountJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletAccountRepositoryAdapter implements WalletAccountRepository {

    private final WalletAccountJpaRepository jpa;

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<WalletAccount> findByUserId(Long userId) {
        return jpa.findByUserId(userId).map(WalletJpaMapper::toDomain);
    }

    @Override
    @Transactional
    public WalletAccount save(WalletAccount account) {
        var e = WalletJpaMapper.toEntity(account);
        e = e.toBuilder()
                .user(em.getReference(UserEntity.class, account.getUserId()))
                .build();
        var saved = jpa.save(e);
        return WalletJpaMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public WalletAccount createIfNotExists(Long userId) {
        return jpa.findByUserId(userId)
                .map(WalletJpaMapper::toDomain)
                .orElseGet(() -> {
                    UserEntity userRef = em.getReference(UserEntity.class, userId);
                    WalletAccountEntity entity = WalletAccountEntity.create(userRef);
                    return WalletJpaMapper.toDomain(jpa.save(entity));
                });
    }
}