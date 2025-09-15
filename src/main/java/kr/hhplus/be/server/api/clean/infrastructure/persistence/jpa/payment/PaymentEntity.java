package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.payment;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.reservation.ReservationEntity;
import kr.hhplus.be.server.common.enums.Actor;
import kr.hhplus.be.server.common.enums.PaymentStatus;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "payments")
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentEntity extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 예약 연관관계
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private ReservationEntity reservation;

    @NotNull
    @Column(name = "amount", nullable = false)
    private Long amount;

    @Size(max = 50)
    @Column(name = "provider", length = 50)
    private String provider;

    @Size(max = 100)
    @Column(name = "provider_txn_id", length = 100)
    private String providerTxnId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "succeeded_at")
    private LocalDateTime succeededAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @NotNull
    @Size(max = 64) // 36도 충분하지만 여유
    @Column(name = "idempotency_key", nullable = false, length = 64, unique = true)
    private String idempotencyKey;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.PERSIST, orphanRemoval = false)
    private List<PaymentStatusHistoryEntity> histories = new ArrayList<>();

    /* ====== 팩토리/도메인 메서드 ====== */

    public static PaymentEntity createPending(ReservationEntity reservation,
                                              Long amount,
                                              String idempotencyKey,
                                              Actor actor,
                                              String message) {
        PaymentEntity p = new PaymentEntity();
        p.reservation = reservation;
        p.amount = amount;
        p.status = PaymentStatus.PENDING;
        p.idempotencyKey = idempotencyKey;
        p.appendHistory(null, PaymentStatus.PENDING, actor, "CREATED", message);
        return p;
    }

    public void succeed(Actor actor, String message) {
        changeStatus(PaymentStatus.SUCCEEDED, actor, "SUCCESS", message);
        this.succeededAt = LocalDateTime.now();
        this.failedAt = null;
    }

    public void fail(Actor actor, String reasonCode, String message) {
        changeStatus(PaymentStatus.FAILED, actor, reasonCode, message);
        this.failedAt = LocalDateTime.now();
        this.succeededAt = null;
    }


    private void changeStatus(PaymentStatus newStatus, Actor actor, String reasonCode, String message) {
        if (this.status == newStatus) return; // 멱등 방지(선택)
        PaymentStatus prev = this.status;
        this.status = newStatus;
        appendHistory(prev, newStatus, actor, reasonCode, message);
    }

    private void appendHistory(PaymentStatus prev,
                               PaymentStatus now,
                               Actor actor,
                               String reasonCode,
                               String message) {
        PaymentStatusHistoryEntity h = PaymentStatusHistoryEntity.of(this, prev, now, actor, reasonCode, message);
    }
}