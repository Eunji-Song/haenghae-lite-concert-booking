package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.reservation;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import kr.hhplus.be.server.api.layered.entity.concert.ConcertDateEntity;
import kr.hhplus.be.server.api.layered.entity.concert.ConcertSeatEntity;
import kr.hhplus.be.server.api.layered.entity.user.UserEntity;
import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationEntity extends BaseEntity {
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
    @JoinColumn(name = "concert_date_id", nullable = false)
    private ConcertDateEntity concertDate;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ConcertSeatEntity concertSeats;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @NotNull
    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @NotNull
    @Column(name = "version", nullable = false)
    private Long version;

}