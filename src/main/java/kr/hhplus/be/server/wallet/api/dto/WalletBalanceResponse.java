package kr.hhplus.be.server.wallet.api.dto;


import java.time.LocalDateTime;



public record WalletBalanceResponse(
        long balance,
        String currency,
        LocalDateTime lastUpdatedAt
) {}