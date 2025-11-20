package kr.hhplus.be.server.payment.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.hhplus.be.server.common.enums.WalletTransactionType;
import kr.hhplus.be.server.common.exception.wallet.WalletNotFoundException;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.payment.application.port.out.WalletPort;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.entity.PaymentEntity;
import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.IdempotencyKeyEntity;
import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.WalletAccountEntity;
import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.WalletTransactionEntity;
import kr.hhplus.be.server.wallet.infrastructure.jpa.repository.IdempotencyKeyJpaRepository;
import kr.hhplus.be.server.wallet.infrastructure.jpa.repository.WalletAccountJpaRepository;
import kr.hhplus.be.server.wallet.infrastructure.jpa.repository.WalletTransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class WalletAdapter implements WalletPort {

    private final WalletAccountJpaRepository walletAccountRepo;
    private final WalletTransactionJpaRepository walletTxnRepo;
    private final IdempotencyKeyJpaRepository idempoRepo;

    @PersistenceContext
    private EntityManager em;

    @Override
    public void debit(Long userId, Long amount, Long relatedPaymentId, String idempotencyKey) {
        validateAmount(amount);

        WalletAccountEntity account = walletAccountRepo.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);

        String idemKey = normalizeKey("wallet:debit", userId, idempotencyKey);
        if (idemKey != null && idempoRepo.existsByRequestKey(idemKey)) return;

        if (account.getBalance() < amount) {
            throw new IllegalStateException("Insufficient wallet balance");
        }
        account.debit(amount);
        walletAccountRepo.save(account);

        WalletTransactionEntity tx = WalletTransactionEntity.of(
                em.getReference(UserEntity.class, userId),
                relatedPaymentId != null ? em.getReference(PaymentEntity.class, relatedPaymentId) : null,
                amount,
                WalletTransactionType.DEBIT,
                idempotencyKey
        );
        walletTxnRepo.save(tx);

        if (idemKey != null) idempoRepo.save(IdempotencyKeyEntity.of(idemKey));
    }

    @Override
    public void refund(Long userId, Long amount, Long relatedPaymentId, String idempotencyKey) {
        validateAmount(amount);

        WalletAccountEntity account = walletAccountRepo.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);

        String idemKey = normalizeKey("wallet:refund", userId, idempotencyKey);
        if (idemKey != null && idempoRepo.existsByRequestKey(idemKey)) return;

        account.charge(amount);
        walletAccountRepo.save(account);

        WalletTransactionEntity tx = WalletTransactionEntity.of(
                em.getReference(UserEntity.class, userId),
                relatedPaymentId != null ? em.getReference(PaymentEntity.class, relatedPaymentId) : null,
                amount,
                WalletTransactionType.REFUND,
                idempotencyKey
        );
        walletTxnRepo.save(tx);

        if (idemKey != null) idempoRepo.save(IdempotencyKeyEntity.of(idemKey));
    }

    private static void validateAmount(Long amount) {
        if (amount == null || amount <= 0) throw new IllegalArgumentException("amount must be positive");
    }

    private static String normalizeKey(String prefix, Long userId, String key) {
        if (key == null || key.isBlank()) return null;
        return prefix + ":" + userId + ":" + key.trim();
    }
}