package kr.hhplus.be.server.api.layered.entity.queue;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.hhplus.be.server.common.entity.concert.ConcertEntity;
import kr.hhplus.be.server.api.layered.entity.user.UserEntity;
import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;


@Entity
@Table(name = "queue_audit_logs")
@Getter
@Builder
@SQLDelete(sql = "UPDATE queue_audit_logs SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@NoArgsConstructor
@AllArgsConstructor
public class QueueAuditLogEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    private UserEntity user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concert_id", nullable = false)
    private ConcertEntity concert;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QueueStatus status;

    @Column(name = "rank_position", columnDefinition = "int UNSIGNED")
    private Long rankPosition;

    @Size(max = 255)
    @Column(name = "note")
    private String note;

}