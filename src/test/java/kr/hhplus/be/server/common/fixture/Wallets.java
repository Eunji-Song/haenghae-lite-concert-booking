package kr.hhplus.be.server.common.fixture;

import kr.hhplus.be.server.common.enums.WalletTransactionType;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.wallet.domain.model.WalletAccount;
import kr.hhplus.be.server.wallet.domain.model.WalletTransaction;
import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.WalletAccountEntity;
import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.WalletTransactionEntity;

import java.time.LocalDateTime;

public final class Wallets {
    private Wallets(){}

    // -------- domain --------
    public static WalletAccount accountDomain(Long userId, long balance) {
        return new WalletAccount(userId, balance, LocalDateTime.now());
    }

    public static WalletTransaction txnDomain(Long userId, Long paymentId, long amt, WalletTransactionType type) {
        return new WalletTransaction(null, userId, paymentId, amt, type, null, LocalDateTime.now());
    }

    // -------- entity --------
    public static WalletAccountEntity accountEntity(UserEntity user, long balance) {
        var e = WalletAccountEntity.create(user);
        if (balance > 0) e.charge(balance);
        return e;
    }

    public static WalletTransactionEntity txnEntity(UserEntity user, long amt, WalletTransactionType type) {
        return WalletTransactionEntity.builder()
                .user(user)
                .amount(amt)
                .type(type)
                .build();
    }
}