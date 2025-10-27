package kr.hhplus.be.server.wallet.infrastructure.jpa.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_idempotency_request_key", columnNames = "request_key")
        },
        indexes = {
                @Index(name = "idx_idempotency_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@SQLDelete(sql = "UPDATE idempotency_keys SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class IdempotencyKeyEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    private Long id;

    @Column(name = "request_key", nullable = false, length = 128)
    @Comment("요청 멱등 키(엔드포인트+사용자)")
    private String requestKey;

    public static IdempotencyKeyEntity of(String requestKey) {
        return IdempotencyKeyEntity.builder().requestKey(requestKey).build();
    }
}