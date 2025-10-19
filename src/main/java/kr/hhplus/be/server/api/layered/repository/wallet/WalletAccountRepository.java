package kr.hhplus.be.server.api.layered.repository.wallet;

import kr.hhplus.be.server.api.layered.entity.wallet.WalletAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletAccountRepository extends JpaRepository<WalletAccountEntity, Long> {
    Optional<WalletAccountEntity> findByUserId(Long userId);
}