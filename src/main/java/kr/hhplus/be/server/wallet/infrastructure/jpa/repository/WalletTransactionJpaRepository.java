package kr.hhplus.be.server.wallet.infrastructure.jpa.repository;

import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.WalletTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WalletTransactionJpaRepository extends JpaRepository<WalletTransactionEntity, Long> {

    @Query("SELECT max(w.createdAt) FROM WalletTransactionEntity w WHERE w.user.id = :userId")
    Optional<LocalDateTime> findTopCreatedAtByUserIdOrderByCreatedAtDesc(Long userId);
}