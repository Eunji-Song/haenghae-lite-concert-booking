package kr.hhplus.be.server.wallet.infrastructure.jpa.mapper;

import kr.hhplus.be.server.common.enums.WalletTransactionType;
import kr.hhplus.be.server.wallet.domain.model.WalletAccount;
import kr.hhplus.be.server.wallet.domain.model.WalletTransaction;
import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.WalletAccountEntity;
import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.WalletTransactionEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WalletJpaMapper {

    public static WalletAccount toDomain(WalletAccountEntity e) {
        if (e == null) return null;
        return new WalletAccount(
                e.getUserId(),
                e.getBalance(),
                e.getCreatedAt()
        );
    }

    public static WalletAccountEntity toEntity(WalletAccount d) {
        if (d == null) return null;
        // 🔧 빌더 사용 (user는 어댑터에서 set)
        return WalletAccountEntity.builder()
                .userId(d.getUserId())
                .balance(d.getBalance())
                .build();
    }

    public static WalletTransaction toDomain(WalletTransactionEntity e) {
        if (e == null) return null;
        Long paymentId = (e.getRelatedPayment() != null ? e.getRelatedPayment().getId() : null);
        return new WalletTransaction(
                e.getId(),
                e.getUser().getId(),
                paymentId,
                e.getAmount(),
                e.getType(),
                e.getIdempotencyKey(),
                e.getCreatedAt()
        );
    }

    public static WalletTransactionEntity toEntity(WalletTransaction d) {
        if (d == null) return null;
        return WalletTransactionEntity.builder()
                .id(d.getId())
                .amount(d.getAmount())
                .type(d.getType() != null ? d.getType() : WalletTransactionType.CHARGE)
                .idempotencyKey(d.getIdempotencyKey())
                .build();
    }
}