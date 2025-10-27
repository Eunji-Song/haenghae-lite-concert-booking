package kr.hhplus.be.server.identity.infrastructure.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import kr.hhplus.be.server.common.jpa.BaseEntity;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name="uk_users_uuid",  columnNames = "user_uuid"),
                @UniqueConstraint(name="uk_users_email", columnNames = "email")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class UserEntity extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Comment("PK")
    private Long id;

    @Column(name = "user_uuid", nullable = false, length = 36, updatable = false)
    @Comment("외부 노출용 UUID")
    private String userUuid;

    @Column(name = "email", nullable = false, length = 255)
    @Comment("로그인 이메일")
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    @Comment("암호 해시")
    private String password;

    @Column(name = "name", nullable = false, length = 50)
    @Comment("사용자 이름")
    private String name;

    // 내부 세터(행위)
    public void changeEmail(String newEmail) { this.email = newEmail; }
    public void changePassword(String newHash) { this.password = newHash; }
    public void rename(String newName) { this.name = newName; }
}