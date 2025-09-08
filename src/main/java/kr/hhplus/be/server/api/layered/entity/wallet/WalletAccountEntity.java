package kr.hhplus.be.server.api.layered.entity.wallet;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import kr.hhplus.be.server.api.layered.entity.user.UserEntity;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wallet_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletAccountEntity extends BaseEntity {
    @Id
    @Column(name = "user_id", nullable = false)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    private UserEntity users;

    @NotNull
    @Column(name = "balance", nullable = false)
    private Long balance;

}