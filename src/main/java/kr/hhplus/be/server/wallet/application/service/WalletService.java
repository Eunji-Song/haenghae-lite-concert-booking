package kr.hhplus.be.server.wallet.application.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.identity.domain.model.User;
import kr.hhplus.be.server.identity.domain.repository.UserRepository;
import kr.hhplus.be.server.wallet.api.dto.WalletBalanceResponse;
import kr.hhplus.be.server.wallet.domain.model.WalletAccount;
import kr.hhplus.be.server.wallet.domain.model.WalletTransaction;
import kr.hhplus.be.server.wallet.domain.repository.IdempotencyKeyRepository;
import kr.hhplus.be.server.wallet.domain.repository.WalletAccountRepository;
import kr.hhplus.be.server.wallet.domain.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class WalletService {

    private final UserRepository userRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Transactional(Transactional.TxType.SUPPORTS)
    public WalletBalanceResponse getBalance(String userUuid) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userUuid));

        WalletAccount account = walletAccountRepository.findByUserId(user.getId())
                .orElseGet(() -> walletAccountRepository.createIfNotExists(user.getId()));

        LocalDateTime last = walletTransactionRepository.findLatestCreatedAtByUserId(user.getId())
                .orElse(account.getCreatedAt());

        return new WalletBalanceResponse(account.getBalance(), "KRW", last);
    }

    public WalletBalanceResponse charge(String userUuid, long amount, String idempotencyKey) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userUuid));

        // 멱등 처리
        if (idempotencyKeyRepository.exists(idempotencyKey)) {
            WalletAccount acct = walletAccountRepository.findByUserId(user.getId())
                    .orElseGet(() -> walletAccountRepository.createIfNotExists(user.getId()));
            LocalDateTime last = walletTransactionRepository.findLatestCreatedAtByUserId(user.getId())
                    .orElse(acct.getCreatedAt());
            return new WalletBalanceResponse(acct.getBalance(), "KRW", last);
        }

        WalletAccount account = walletAccountRepository.findByUserId(user.getId())
                .orElseGet(() -> walletAccountRepository.createIfNotExists(user.getId()));

        account.charge(amount);
        walletAccountRepository.save(account);

        WalletTransaction txn = WalletTransaction.charge(user.getId(), amount, idempotencyKey);
        walletTransactionRepository.save(txn);

        idempotencyKeyRepository.save(idempotencyKey);

        LocalDateTime last = walletTransactionRepository.findLatestCreatedAtByUserId(user.getId())
                .orElse(account.getCreatedAt());

        return new WalletBalanceResponse(account.getBalance(), "KRW", last);
    }
}