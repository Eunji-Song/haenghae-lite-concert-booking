package kr.hhplus.be.server.identity.infrastructure.jpa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import kr.hhplus.be.server.common.jpa.CreatedOnlyEntity;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name="uk_user_tokens_rth", columnNames = "refresh_token_hash")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserTokenEntity extends CreatedOnlyEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", nullable=false)
    @Comment("PK")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="user_id", nullable=false,
            foreignKey = @ForeignKey(name="fk_user_tokens_user"))
    @Comment("FK: users.id")
    private UserEntity user;

    @Column(name="refresh_token_hash", nullable=false, length=255)
    @Comment("리프레시 토큰 해시")
    private String refreshTokenHash;

    @Column(name="expires_at", nullable=false)
    @Comment("만료 시각(UTC)")
    private LocalDateTime expiresAt;

    // 내부 세터
    public void rotate(String newHash, LocalDateTime newExpiry){
        this.refreshTokenHash = newHash;
        this.expiresAt = newExpiry;
    }
}