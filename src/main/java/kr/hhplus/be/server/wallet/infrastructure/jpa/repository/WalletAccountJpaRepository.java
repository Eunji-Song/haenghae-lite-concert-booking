package kr.hhplus.be.server.wallet.infrastructure.jpa.repository;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.WalletAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WalletAccountJpaRepository extends JpaRepository<WalletAccountEntity, Long> {
    Optional<WalletAccountEntity> findByUserId(Long userId);

    @Query("select w from WalletAccountEntity w where w.userId = :userId")
    Optional<WalletAccountEntity> findByIdForUpdate(@Param("userId") Long userId);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select w from WalletAccountEntity w where w.userId = :userId")
    Optional<WalletAccountEntity> findByIdForUpdateOptimistic(@Param("userId") Long userId);


    @Modifying
    @Query(value = """
            INSERT INTO wallet_accounts (user_id, balance, created_at, version)
            VALUES (:userId, 0, CURRENT_TIMESTAMP(6), 0)
            ON DUPLICATE KEY UPDATE user_id = user_id
            """, nativeQuery = true)
    int upsertBlankAccount(@Param("userId") Long userId);

}