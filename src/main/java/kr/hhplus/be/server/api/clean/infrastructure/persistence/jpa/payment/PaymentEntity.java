package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.payment;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.reservation.ReservationEntity;
import kr.hhplus.be.server.common.enums.PaymentStatus;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

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

    @Size(max = 36)
    @NotNull
    @Column(name = "idempotency_key", nullable = false, length = 36)
    private String idempotencyKey;

}