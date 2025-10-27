package kr.hhplus.be.server.wallet.infrastructure.jpa.adapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.entity.PaymentEntity;
import kr.hhplus.be.server.wallet.domain.model.WalletTransaction;
import kr.hhplus.be.server.wallet.domain.repository.WalletTransactionRepository;
import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.WalletTransactionEntity;
import kr.hhplus.be.server.wallet.infrastructure.jpa.mapper.WalletJpaMapper;
import kr.hhplus.be.server.wallet.infrastructure.jpa.repository.WalletTransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletTransactionRepositoryAdapter implements WalletTransactionRepository {

    private final WalletTransactionJpaRepository jpa;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public WalletTransaction save(WalletTransaction tx) {
        WalletTransactionEntity e = WalletJpaMapper.toEntity(tx);
        // 관계 주입 (user, payment)
        e = e.toBuilder()
                .user(em.getReference(UserEntity.class, tx.getUserId()))
                .relatedPayment(tx.getRelatedPaymentId() != null
                        ? em.getReference(PaymentEntity.class, tx.getRelatedPaymentId())
                        : null)
                .build();
        return WalletJpaMapper.toDomain(jpa.save(e));
    }

    @Override
    public Optional<LocalDateTime> findLatestCreatedAtByUserId(Long userId) {
        return jpa.findTopCreatedAtByUserIdOrderByCreatedAtDesc(userId);
    }
}