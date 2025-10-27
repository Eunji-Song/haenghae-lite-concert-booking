package kr.hhplus.be.server.wallet.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class WalletAccount {
    private final Long userId;
    private long balance;
    private final LocalDateTime createdAt;

    public WalletAccount(Long userId, long balance, LocalDateTime createdAt) {
        this.userId = userId;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public void charge(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("충전 금액은 1원 이상이어야 합니다.");
        this.balance = Math.addExact(this.balance, amount);
    }

    public void debit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("차감 금액은 1원 이상이어야 합니다.");
        if (this.balance < amount) throw new IllegalStateException("잔액 부족");
        this.balance -= amount;
    }
}