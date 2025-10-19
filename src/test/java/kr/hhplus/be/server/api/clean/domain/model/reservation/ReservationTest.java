package kr.hhplus.be.server.api.clean.domain.model.reservation;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.reservation.InvalidReservationStatusException;
import kr.hhplus.be.server.common.exception.reservation.ReservationAlreadyCanceledException;
import kr.hhplus.be.server.common.exception.reservation.ReservationHoldExpiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Reservation 도메인 테스트")
class ReservationTest {

    @Test
    void PENDING_상태의_신규예약_생성() {
        // Given
        Long userId = 1L;
        Long concertDateId = 1L;
        Long seatId = 1L;
        Long amount = 50000L;
        LocalDateTime beforeCreation = LocalDateTime.now();

        // When
        Reservation reservation = Reservation.createPendingReservation(userId, concertDateId, seatId, amount);

        // Then
        assertThat(reservation.getId()).isNull(); // 새 생성시 ID는 null
        assertThat(reservation.getUserId()).isEqualTo(userId);
        assertThat(reservation.getConcertDateId()).isEqualTo(concertDateId);
        assertThat(reservation.getSeatId()).isEqualTo(seatId);
        assertThat(reservation.getAmount()).isEqualTo(amount);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.getHoldExpiresAt()).isAfter(beforeCreation);
        assertThat(reservation.getConfirmedAt()).isNull();
        assertThat(reservation.getCanceledAt()).isNull();
        assertThat(reservation.getVersion()).isEqualTo(0L);
    }

    @Test
    void PENDING_상태의_예약_확정으로_업데이트() {
        // Given
        Reservation reservation = createValidPendingReservation();
        LocalDateTime beforeConfirm = LocalDateTime.now();

        // When
        reservation.confirm();

        // Then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getConfirmedAt()).isAfter(beforeConfirm);
        assertThat(reservation.getHoldExpiresAt()).isNull(); // 확정되면 홀드 시간 제거
    }

    @Test
    @DisplayName("PENDING이 아닌 상태에서는 확정할 수 없다")
    void confirm_FromNonPending_ThrowsException() {
        // Given
        Reservation reservation = createValidPendingReservation();
        reservation.cancel(); // CANCELED 상태로 변경

        // When & Then
        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(InvalidReservationStatusException.class)
                .hasMessage("PENDING 상태에서만 확정할 수 있습니다");
    }

    @Test
    @DisplayName("홀드 시간이 만료된 예약은 확정할 수 없다")
    void confirm_ExpiredHold_ThrowsException() {
        // Given
        Reservation reservation = createExpiredReservation();

        // When & Then
        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(ReservationHoldExpiredException.class);
    }

    @Test
    @DisplayName("예약을 취소할 수 있다")
    void cancel_Success() {
        // Given
        Reservation reservation = createValidPendingReservation();
        LocalDateTime beforeCancel = LocalDateTime.now();

        // When
        reservation.cancel();

        // Then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(reservation.getCanceledAt()).isAfter(beforeCancel);
        assertThat(reservation.getHoldExpiresAt()).isNull(); // 취소되면 홀드 시간 제거
    }

    @Test
    @DisplayName("이미 취소된 예약은 다시 취소할 수 없다")
    void cancel_AlreadyCanceled_ThrowsException() {
        // Given
        Reservation reservation = createValidPendingReservation();
        reservation.cancel(); // 먼저 취소

        // When & Then
        assertThatThrownBy(reservation::cancel)
                .isInstanceOf(ReservationAlreadyCanceledException.class);
    }

    @Test
    @DisplayName("PENDING 상태의 예약을 만료 처리할 수 있다")
    void expire_FromPending_Success() {
        // Given
        Reservation reservation = createValidPendingReservation();

        // When
        reservation.expire();

        // Then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(reservation.getHoldExpiresAt()).isNull();
    }

    @Test
    @DisplayName("PENDING이 아닌 상태는 만료 처리할 수 없다")
    void expire_FromNonPending_ThrowsException() {
        // Given
        Reservation reservation = createValidPendingReservation();
        reservation.confirm(); // CONFIRMED 상태로 변경

        // When & Then
        assertThatThrownBy(reservation::expire)
                .isInstanceOf(InvalidReservationStatusException.class)
                .hasMessage("PENDING 상태만 만료 처리할 수 있습니다");
    }

    @Test
    @DisplayName("홀드 시간이 지나면 만료된 것으로 판단한다")
    void isExpired_AfterHoldTime_ReturnsTrue() {
        // Given
        Reservation reservation = createExpiredReservation();

        // When & Then
        assertThat(reservation.isExpired()).isTrue();
    }

    @Test
    @DisplayName("홀드 시간이 지나지 않으면 만료되지 않은 것으로 판단한다")
    void isExpired_BeforeHoldTime_ReturnsFalse() {
        // Given
        Reservation reservation = createValidPendingReservation();

        // When & Then
        assertThat(reservation.isExpired()).isFalse();
    }

    @Test
    @DisplayName("홀드 시간이 null이면 만료되지 않은 것으로 판단한다")
    void isExpired_NullHoldTime_ReturnsFalse() {
        // Given
        Reservation reservation = createValidPendingReservation();
        reservation.confirm(); // 확정되면 holdExpiresAt이 null이 됨

        // When & Then
        assertThat(reservation.isExpired()).isFalse();
    }

    @Test
    @DisplayName("PENDING과 CONFIRMED 상태는 활성 상태다")
    void isActive_PendingAndConfirmed_ReturnsTrue() {
        // Given
        Reservation pendingReservation = createValidPendingReservation();
        Reservation confirmedReservation = createValidPendingReservation();
        confirmedReservation.confirm();

        // When & Then
        assertThat(pendingReservation.isActive()).isTrue();
        assertThat(confirmedReservation.isActive()).isTrue();
    }

    @Test
    @DisplayName("CANCELED와 EXPIRED 상태는 비활성 상태다")
    void isActive_CanceledAndExpired_ReturnsFalse() {
        // Given
        Reservation canceledReservation = createValidPendingReservation();
        canceledReservation.cancel();

        Reservation expiredReservation = createValidPendingReservation();
        expiredReservation.expire();

        // When & Then
        assertThat(canceledReservation.isActive()).isFalse();
        assertThat(expiredReservation.isActive()).isFalse();
    }

    @Test
    @DisplayName("PENDING 상태이고 만료되지 않은 예약은 결제 가능하다")
    void isPayable_PendingAndNotExpired_ReturnsTrue() {
        // Given
        Reservation reservation = createValidPendingReservation();

        // When & Then
        assertThat(reservation.isPayable()).isTrue();
    }

    @Test
    @DisplayName("만료된 예약은 결제할 수 없다")
    void isPayable_Expired_ReturnsFalse() {
        // Given
        Reservation reservation = createExpiredReservation();

        // When & Then
        assertThat(reservation.isPayable()).isFalse();
    }

    @Test
    @DisplayName("PENDING이 아닌 상태는 결제할 수 없다")
    void isPayable_NonPending_ReturnsFalse() {
        // Given
        Reservation reservation = createValidPendingReservation();
        reservation.confirm();

        // When & Then
        assertThat(reservation.isPayable()).isFalse();
    }

    // === 테스트 헬퍼 메서드들 ===

    private Reservation createValidPendingReservation() {
        return Reservation.createPendingReservation(1L, 1L, 1L, 50000L);
    }

    // 홀딩 시간 만료 건 생성
    private Reservation createExpiredReservation() {
        return new Reservation(
                1L, // id
                1L, // userId
                1L, // concertDateId
                1L, // seatId
                ReservationStatus.PENDING,
                50000L, // amount
                LocalDateTime.now().minusMinutes(10), // 10분 전 만료
                null, // confirmedAt
                null, // canceledAt
                0L // version
        );
    }
}