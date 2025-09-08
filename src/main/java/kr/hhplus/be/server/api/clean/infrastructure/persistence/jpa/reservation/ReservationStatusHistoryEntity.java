package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.reservation;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "reservation_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationStatusHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private ReservationEntity reservation;

    @Lob
    @Column(name = "prev_status")
    private String prevStatus;

    @NotNull
    @Lob
    @Column(name = "new_status", nullable = false)
    private String newStatus;

    @NotNull
    @Lob
    @Column(name = "cause", nullable = false)
    private String cause;

    @Size(max = 255)
    @Column(name = "note")
    private String note;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}