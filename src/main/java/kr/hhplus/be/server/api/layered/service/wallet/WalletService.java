package kr.hhplus.be.server.api.layered.service.wallet;

import kr.hhplus.be.server.api.layered.dto.wallet.WalletBalanceResponse;
import kr.hhplus.be.server.api.layered.dto.wallet.WalletChargeRequest;
import kr.hhplus.be.server.api.layered.entity.user.UserEntity;
import kr.hhplus.be.server.api.layered.entity.wallet.WalletAccountEntity;
import kr.hhplus.be.server.api.layered.entity.wallet.WalletTransactionEntity;
import kr.hhplus.be.server.api.layered.repository.wallet.WalletAccountRepository;
import kr.hhplus.be.server.api.layered.repository.wallet.WalletTransactionRepository;
import kr.hhplus.be.server.api.layered.service.user.UserService;
import kr.hhplus.be.server.common.exception.InvalidChargeAmountException;
import kr.hhplus.be.server.common.exception.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private final UserService userService;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional
    public void createWallet(String userUuid) {
        UserEntity user = userService.getUser(userUuid);

        walletAccountRepository.findByUserId(user.getId())
                .orElseGet(() -> walletAccountRepository.save(
                        WalletAccountEntity.builder()
                                .user(user)
                                .balance(0L)
                                .build()
                ));
    }

    public WalletBalanceResponse getBalance(String userUuid) {
        UserEntity user = userService.getUser(userUuid);

        WalletAccountEntity account = walletAccountRepository.findByUserId(user.getId())
                .orElseThrow(WalletNotFoundException::new);

        LocalDateTime updatedAt = account.getUpdatedAt() != null ? account.getUpdatedAt() : LocalDateTime.now();

        return new WalletBalanceResponse(
                account.getBalance(),
                "KRW",
                updatedAt
        );
    }

    @Transactional
    public WalletBalanceResponse charge(String userUuid, WalletChargeRequest request, String idemKey) {
        Long amount = request.getAmount();

        if (amount <= 0) {
            throw new InvalidChargeAmountException();
        }

        UserEntity user = userService.getUser(userUuid);
        WalletAccountEntity account = walletAccountRepository.findByUserId(user.getId())
                .orElseThrow(WalletNotFoundException::new);

        account.addBalance(amount);
        walletAccountRepository.save(account);

        WalletTransactionEntity txn = WalletTransactionEntity.createCharge(account, amount, idemKey);
        walletTransactionRepository.save(txn);

        return new WalletBalanceResponse(account.getBalance(), "KRW", account.getUpdatedAt());
    }
}