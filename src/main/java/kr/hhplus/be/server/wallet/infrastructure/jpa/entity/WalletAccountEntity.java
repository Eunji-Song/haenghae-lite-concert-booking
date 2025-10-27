package kr.hhplus.be.server.wallet.infrastructure.jpa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import kr.hhplus.be.server.common.jpa.CreatedOnlyEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import lombok.*;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "wallet_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class WalletAccountEntity extends CreatedOnlyEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    @Comment("PK/FK: users.id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_wallet_accounts_user")
    )
    @Comment("연관 사용자")
    private UserEntity user;

    @Min(0)
    @Column(name = "balance", nullable = false)
    @Comment("현재 잔액(원)")
    private long balance;

    public void charge(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("충전 금액은 1원 이상이어야 합니다.");
        this.balance = Math.addExact(this.balance, amount);
    }

    public void debit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("차감 금액은 1원 이상이어야 합니다.");
        if (this.balance < amount) throw new IllegalStateException("잔액 부족");
        this.balance -= amount;
    }

    public static WalletAccountEntity create(UserEntity user) {
        return new WalletAccountEntity(user.getId(), user, 0L);
    }
}