package kr.hhplus.be.server.wallet.application.service;

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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private final UserRepository userRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public WalletBalanceResponse getBalance(String userUuid) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userUuid));

        WalletAccount account = walletAccountRepository.findByUserId(user.id())
                .orElseGet(() -> walletAccountRepository.createIfNotExists(user.id()));

        LocalDateTime last = walletTransactionRepository.findLatestCreatedAtByUserId(user.id())
                .orElse(account.getCreatedAt());

        return new WalletBalanceResponse(account.getBalance(), "KRW", last);
    }

    @Transactional
    public WalletBalanceResponse charge(String userUuid, long amount, String idempotencyKey) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userUuid));


        // 멱등 처리
        if (idempotencyKeyRepository.exists(idempotencyKey)) {
            WalletAccount acct = walletAccountRepository.findByUserId(user.id())
                    .orElseGet(() -> walletAccountRepository.createIfNotExists(user.id()));
            LocalDateTime last = walletTransactionRepository.findLatestCreatedAtByUserId(user.id())
                    .orElse(acct.getCreatedAt());
            return new WalletBalanceResponse(acct.getBalance(), "KRW", last);
        }

        WalletAccount account = walletAccountRepository.findByUserId(user.id())
                .orElseGet(() -> walletAccountRepository.createIfNotExists(user.id()));

        account.charge(amount);
        walletAccountRepository.save(account);

        WalletTransaction txn = WalletTransaction.charge(user.id(), amount, idempotencyKey);
        walletTransactionRepository.save(txn);

        idempotencyKeyRepository.save(idempotencyKey);

        LocalDateTime last = walletTransactionRepository.findLatestCreatedAtByUserId(user.id())
                .orElse(account.getCreatedAt());

        return new WalletBalanceResponse(account.getBalance(), "KRW", last);
    }
}