package kr.hhplus.be.server.wallet.infrastructure.jpa.repository;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.WalletAccountEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface WalletAccountJpaRepository extends JpaRepository<WalletAccountEntity, Long> {

    Optional<WalletAccountEntity> findByUserId(Long userId);

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Query("""
            SELECT w 
              FROM WalletAccountEntity w 
             WHERE w.userId = :userId
            """)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    Optional<WalletAccountEntity> findByIdForUpdate(@Param("userId") Long userId);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("""
            SELECT w 
              FROM WalletAccountEntity w 
             WHERE w.userId = :userId
            """)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    Optional<WalletAccountEntity> findByIdForUpdateOptimistic(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
            INSERT INTO wallet_accounts (user_id, balance, created_at, version)
            VALUES (:userId, 0, CURRENT_TIMESTAMP(6), 0)
            ON DUPLICATE KEY UPDATE user_id = user_id
            """, nativeQuery = true)
    void upsertBlankAccount(@Param("userId") Long userId);
}