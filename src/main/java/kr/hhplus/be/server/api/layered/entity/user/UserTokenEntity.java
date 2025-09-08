package kr.hhplus.be.server.api.layered.entity.user;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_tokens")
@Getter
@SQLDelete(sql = "UPDATE user_tokens SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTokenEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Comment("ID")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    @Comment("USER")
    private UsersEntity user;

    @Size(max = 255)
    @NotNull
    @Column(name = "refresh_token_hash", nullable = false)
    @Comment("리프레시 토큰 해시값")
    private String refreshTokenHash;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    @Comment("만료 일시")
    private LocalDateTime expiresAt;

}