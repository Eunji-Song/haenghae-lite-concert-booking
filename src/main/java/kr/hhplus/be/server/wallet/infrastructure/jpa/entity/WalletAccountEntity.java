package kr.hhplus.be.server.wallet.infrastructure.jpa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import kr.hhplus.be.server.common.jpa.CreatedOnlyEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "wallet_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class WalletAccountEntity extends CreatedOnlyEntity implements Persistable<Long> {

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

    @Version
    @Column(name = "version", nullable = false)
    @Comment("낙관적 락 버전")
    private Long version;

    /** Persistable 구현: 신규 여부를 version==null 기준으로 판단 */
    @Override
    public Long getId() {
        return this.userId;
    }

    @Override
    public boolean isNew() {
        return this.version == null;
    }

    /** 지갑 충전 */
    public void charge(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("충전 금액은 1원 이상이어야 합니다.");
        this.balance = Math.addExact(this.balance, amount);
    }

    /** 지갑 차감 */
    public void debit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("차감 금액은 1원 이상이어야 합니다.");
        if (this.balance < amount) throw new IllegalStateException("잔액이 부족합니다.");
        this.balance -= amount;
    }

    /** 팩토리: version 절대 세팅하지 말 것! (null 유지) */
    public static WalletAccountEntity create(UserEntity user) {
        return WalletAccountEntity.builder()
                .userId(user.getId())
                .user(user)
                .balance(0L)
                .build();
    }

    public void setBalance(long balance) {
        if (balance < 0) throw new IllegalArgumentException("잔액은 0 미만일 수 없습니다.");
        this.balance = balance;
    }
}