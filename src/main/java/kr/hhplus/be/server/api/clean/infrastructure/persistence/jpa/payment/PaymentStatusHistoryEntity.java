package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.payment;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentStatusHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private PaymentEntity payment;

    @Lob
    @Column(name = "prev_status")
    private Enum prevStatus;

    @NotNull
    @Lob
    @Column(name = "new_status", nullable = false)
    private String newStatus;

    @Size(max = 50)
    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @Size(max = 255)
    @Column(name = "message")
    private String message;

    @NotNull
    @Lob
    @Column(name = "actor", nullable = false)
    private String actor;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}