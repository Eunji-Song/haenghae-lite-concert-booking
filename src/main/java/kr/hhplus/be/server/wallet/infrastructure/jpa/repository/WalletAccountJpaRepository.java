package kr.hhplus.be.server.wallet.infrastructure.jpa.repository;

import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.WalletAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletAccountJpaRepository extends JpaRepository<WalletAccountEntity, Long> {
    Optional<WalletAccountEntity> findByUserId(Long userId);
}