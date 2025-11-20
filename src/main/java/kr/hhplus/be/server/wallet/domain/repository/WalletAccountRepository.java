package kr.hhplus.be.server.wallet.domain.repository;

import kr.hhplus.be.server.wallet.domain.model.WalletAccount;

import java.util.Optional;

public interface WalletAccountRepository {
    Optional<WalletAccount> findByUserId(Long userId);
    WalletAccount save(WalletAccount account);
    WalletAccount createIfNotExists(Long userId);


}