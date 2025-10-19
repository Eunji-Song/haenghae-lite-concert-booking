package kr.hhplus.be.server.api.clean.domain.model.reservation;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.reservation.InvalidReservationStatusException;
import kr.hhplus.be.server.common.exception.reservation.ReservationAlreadyCanceledException;
import kr.hhplus.be.server.common.exception.reservation.ReservationHoldExpiredException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 순수 도메인 모델: 예약
 * - JPA 어노테이션/엔티티 참조 없음
 * - 외래키는 id(Long)로만 보유
 * - 핵심 비즈니스 로직 포함
 */
@Getter
public class Reservation {
    private Long id;
    private Long userId;
    private Long concertDateId;
    private Long seatId;
    private ReservationStatus status;
    private Long amount;
    private LocalDateTime holdExpiresAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime canceledAt;
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Reservation(Long id, Long userId, Long concertDateId, Long seatId, ReservationStatus status, Long amount, LocalDateTime holdExpiresAt, LocalDateTime confirmedAt, LocalDateTime canceledAt, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.concertDateId = concertDateId;
        this.seatId = seatId;
        this.status = status;
        this.amount = amount;
        this.holdExpiresAt = holdExpiresAt;
        this.confirmedAt = confirmedAt;
        this.canceledAt = canceledAt;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private Reservation(Long userId, Long concertDateId, Long seatId, Long amount) {
        this.id = null;
        this.userId = userId;
        this.concertDateId = concertDateId;
        this.seatId = seatId;
        this.amount = amount;
        this.status = ReservationStatus.PENDING;
        this.holdExpiresAt = LocalDateTime.now().plusMinutes(5); // 5분 홀드
        this.confirmedAt = null;
        this.canceledAt = null;
        this.version = 0L;
    }

    public Reservation(Long id, Long userId, Long concertDateId, Long seatId, ReservationStatus status, Long amount, LocalDateTime holdExpiresAt, LocalDateTime confirmedAt, LocalDateTime canceledAt, Long version) {
        this.id = id;
        this.userId = userId;
        this.concertDateId = concertDateId;
        this.seatId = seatId;
        this.status = status;
        this.amount = amount;
        this.holdExpiresAt = holdExpiresAt;
        this.confirmedAt = confirmedAt;
        this.canceledAt = canceledAt;
        this.version = version;
    }

    // 팩토리 메서드
    public static Reservation createPendingReservation(Long userId, Long concertDateId, Long seatId, Long amount) {
        return new Reservation(userId, concertDateId, seatId, amount);
    }

    /**
     * 예약 확정
     */
    public void confirm() {
        if (this.status != ReservationStatus.PENDING) {
            throw new InvalidReservationStatusException("PENDING 상태에서만 확정할 수 있습니다");
        }
        if (isExpired()) {
            throw new ReservationHoldExpiredException();
        }

        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.holdExpiresAt = null; // 확정되면 홀드 시간 제거
    }

    /**
     * 홀드 시간 만료 여부 확인
     */
    public boolean isExpired() {
        return holdExpiresAt != null && LocalDateTime.now().isAfter(holdExpiresAt);
    }

    /**
     * 예약 취소
     */
    public void cancel() {
        if (this.status == ReservationStatus.CANCELED) {
            throw new ReservationAlreadyCanceledException();
        }

        this.status = ReservationStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.holdExpiresAt = null; // 취소되면 홀드 시간 제거
    }

    /**
     * 예약 만료 처리 (배치 작업용)
     */
    public void expire() {
        if (this.status != ReservationStatus.PENDING) {
            throw new InvalidReservationStatusException("PENDING 상태만 만료 처리할 수 있습니다");
        }

        this.status = ReservationStatus.EXPIRED;
        this.holdExpiresAt = null;
    }

    /**
     * 예약이 활성 상태인지 확인 (좌석 점유 여부 판단용)
     */
    public boolean isActive() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }

    /**
     * 결제 가능한 상태인지 확인
     */
    public boolean isPayable() {
        return status == ReservationStatus.PENDING && !isExpired();
    }

    // 상태 변경을 위한 내부 메서드
    private void changeStatus(ReservationStatus newStatus) {
        this.status = newStatus;
    }
}