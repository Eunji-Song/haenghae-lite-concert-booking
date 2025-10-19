package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.payment;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.hhplus.be.server.common.enums.Actor;
import kr.hhplus.be.server.common.enums.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_status_history")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentStatusHistoryEntity {


    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private PaymentEntity payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "prev_status", length = 20)
    private PaymentStatus prevStatus; // null 허용

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private PaymentStatus newStatus;

    @Size(max = 50)
    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @Size(max = 255)
    @Column(name = "message")
    private String message;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "actor", nullable = false, length = 20)
    private Actor actor;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    private PaymentStatusHistoryEntity(PaymentEntity payment,
                                       PaymentStatus prevStatus,
                                       PaymentStatus newStatus,
                                       Actor actor,
                                       String reasonCode,
                                       String message) {
        this.payment = payment;
        this.prevStatus = prevStatus;
        this.newStatus = newStatus;
        this.actor = actor;
        this.reasonCode = reasonCode;
        this.message = message;
    }

    public static PaymentStatusHistoryEntity of(
            PaymentEntity payment, PaymentStatus prev, PaymentStatus now,
            Actor actor, String reasonCode, String message) {
        return new PaymentStatusHistoryEntity(payment, prev, now, actor, reasonCode, message);
    }
}