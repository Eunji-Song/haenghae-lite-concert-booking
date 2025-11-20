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
        WalletAccountEntity wallet = jpa.findById(account.getUserId())
                .orElseGet(() -> {
                    UserEntity ref = em.getReference(UserEntity.class, account.getUserId());
                    return jpa.saveAndFlush(WalletAccountEntity.create(ref));
                });

        wallet.setBalance(account.getBalance());

        return WalletJpaMapper.toDomain(wallet);
    }

    @Override
    @Transactional
    public WalletAccount createIfNotExists(Long userId) {
        // 1) 빠른 경로: 이미 있으면 그대로 반환
        var existing = jpa.findByUserId(userId);
        if (existing.isPresent()) return WalletJpaMapper.toDomain(existing.get());

        // 2) 없으면 UPSERT로 "있으면 패스/없으면 생성"
        jpa.upsertBlankAccount(userId);

        // 3) 다시 조회해서 도메인으로 반환
        var justCreated = jpa.findByUserId(userId)
                .orElseGet(() -> {
                    // 이 경우는 거의 없지만, 방어적으로 한번 더 보장
                    UserEntity ref = em.getReference(UserEntity.class, userId);
                    var e = WalletAccountEntity.create(ref);
                    return jpa.saveAndFlush(e);
                });

        return WalletJpaMapper.toDomain(justCreated);
    }
}