package kr.hhplus.be.server.api.layered.repository.wallet;

import kr.hhplus.be.server.api.layered.entity.wallet.WalletTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransactionEntity, Long> {
}