package kr.hhplus.be.server.api.layered.entity.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import kr.hhplus.be.server.common.jpa.BaseEntity;

@Entity
@Table(name = "users")
@Getter
@Builder
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Comment("사용자 ID")
    private Long id;

    @Column(name = "user_uuid", nullable = false, length = 36, updatable = false)
    @Comment("사용자 UUID")
    private String userUuid;

    @Email(message = "올바른 이메일 형식이어야 합니다.")
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Size(max = 255, message = "이메일은 255자 이내여야 합니다.")
    @Column(nullable = false, length = 255)
    @Comment("이메일(로그인 ID)")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 8, max = 255, message = "비밀번호는 최소 8자 이상, 최대 255자 이내여야 합니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z].*[A-Za-z]|.*\\d.*\\d|.*[!@#$%^&*()_+\\-={}\\[\\]|:;\"'<>,.?/`~\\\\].*[!@#$%^&*()_+\\-={}\\[\\]|:;\"'<>,.?/`~\\\\]).{8,}$",
            message = "비밀번호는 최소 8자이며, 대문자/소문자/숫자/특수문자 중 2종 이상을 포함해야 합니다."
    )
    @Column(nullable = false, length = 255)
    @Comment("비밀번호(해시값)")
    private String password;

    @NotBlank(message = "이름은 필수 입력값입니다.")
    @Size(max = 50, message = "이름은 50자 이내여야 합니다.")
    @Column(nullable = false, length = 50)
    @Comment("사용자 이름")
    private String name;

}
