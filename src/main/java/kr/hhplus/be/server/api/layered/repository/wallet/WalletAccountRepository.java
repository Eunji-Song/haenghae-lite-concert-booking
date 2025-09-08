package kr.hhplus.be.server.api.layered.repository.wallet;

import kr.hhplus.be.server.api.layered.entity.wallet.WalletAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletAccountRepository extends JpaRepository<WalletAccountEntity, Long> {
}