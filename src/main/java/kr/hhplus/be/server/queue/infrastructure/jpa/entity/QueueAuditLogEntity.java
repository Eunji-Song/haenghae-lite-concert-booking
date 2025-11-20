package kr.hhplus.be.server.queue.infrastructure.jpa.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.common.jpa.CreatedOnlyEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "queue_audit_logs",
        indexes = {
                @Index(name = "idx_queue_logs_user", columnList = "user_id"),
                @Index(name = "idx_queue_logs_concert", columnList = "concert_id"),
                @Index(name = "idx_queue_logs_status", columnList = "status")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QueueAuditLogEntity extends CreatedOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_queue_logs_user"))
    @Comment("FK: users.id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concert_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_queue_logs_concert"))
    @Comment("FK: concerts.id")
    private ConcertEntity concert;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Comment("토큰 상태(ISSUED/ACTIVE/EXPIRED)")
    private QueueStatus status;

    @Column(name = "rank_position")
    @Comment("대기 순번(0이 맨 앞)")
    private Integer rankPosition;

    @Column(name = "note", length = 255)
    @Comment("비고")
    private String note;

    public static QueueAuditLogEntity of(UserEntity user, ConcertEntity concert,
                                         QueueStatus status, Integer rankPosition, String note) {
        QueueAuditLogEntity e = new QueueAuditLogEntity();
        e.user = user;
        e.concert = concert;
        e.status = status;
        e.rankPosition = rankPosition;
        e.note = note;
        return e;
    }
}