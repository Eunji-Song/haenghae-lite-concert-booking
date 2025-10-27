package kr.hhplus.be.server.wallet.domain.repository;

import kr.hhplus.be.server.wallet.domain.model.WalletTransaction;


import java.time.LocalDateTime;
import java.util.Optional;

public interface WalletTransactionRepository {
    WalletTransaction save(WalletTransaction tx);
    Optional<LocalDateTime> findLatestCreatedAtByUserId(Long userId);
}