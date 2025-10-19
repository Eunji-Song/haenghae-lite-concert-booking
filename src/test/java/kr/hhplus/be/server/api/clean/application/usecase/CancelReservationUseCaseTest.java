package kr.hhplus.be.server.api.clean.application.usecase;

import kr.hhplus.be.server.api.clean.application.port.in.reservation.CancelReservationCommand;
import kr.hhplus.be.server.api.clean.application.port.in.reservation.CancelReservationResponse;
import kr.hhplus.be.server.api.clean.application.port.out.payment.PaymentRepository;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.clean.domain.model.payment.Payment;
import kr.hhplus.be.server.api.clean.domain.model.reservation.Reservation;
import kr.hhplus.be.server.common.enums.PaymentStatus;
import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.ReservationAlreadyCanceledException;
import kr.hhplus.be.server.common.exception.reservation.ReservationNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelReservationUseCase 테스트")
class CancelReservationUseCaseTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private CancelReservationUseCase useCase;

    @Test
    void PENDING_상태_예약_취소_성공() {
        // Given
        CancelReservationCommand command = new CancelReservationCommand(1L, 1L);
        Reservation pendingReservation = createPendingReservation(1L, 1L);
        Reservation canceledReservation = createCanceledReservation(1L, 1L);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));
        when(paymentRepository.findByReservationId(1L)).thenReturn(Collections.emptyList());
        when(reservationRepository.save(any(Reservation.class))).thenReturn(canceledReservation);

        // When
        CancelReservationResponse result = useCase.cancelReservation(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.canceled()).isTrue();

        verify(reservationRepository).findById(1L);
        verify(paymentRepository).findByReservationId(1L);
        verify(reservationRepository).save(argThat(reservation ->
                reservation.getStatus() == ReservationStatus.CANCELED &&
                        reservation.getCanceledAt() != null
        ));
    }

    @Test
    void CONFIRMED_상태_예약_취소_및_환불_성공() {
        // Given
        CancelReservationCommand command = new CancelReservationCommand(1L, 1L);
        Reservation confirmedReservation = createConfirmedReservation(1L, 1L);
        Payment successfulPayment = createSuccessfulPayment(1L, 1L);
        Reservation canceledReservation = createCanceledReservation(1L, 1L);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(confirmedReservation));
        when(paymentRepository.findByReservationId(1L)).thenReturn(List.of(successfulPayment));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(canceledReservation);

        // When
        CancelReservationResponse result = useCase.cancelReservation(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.canceled()).isTrue();

        verify(reservationRepository).findById(1L);
        verify(paymentRepository).findByReservationId(1L);
        verify(reservationRepository).save(any());
        // TODO: 환불 처리 로직 검증 추가
    }

    @Test
    void 존재하지_않는_예약_취소_실패() {
        // Given
        CancelReservationCommand command = new CancelReservationCommand(999L, 1L);

        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.cancelReservation(command))
                .isInstanceOf(ReservationNotFoundException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void 다른_사용자_예약_취소_권한_없음() {
        // Given
        CancelReservationCommand command = new CancelReservationCommand(1L, 2L); // userId=2L로 시도
        Reservation otherUserReservation = createPendingReservation(1L, 1L); // userId=1L의 예약

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(otherUserReservation));

        // When & Then
        assertThatThrownBy(() -> useCase.cancelReservation(command))
                .isInstanceOf(ReservationAccessDeniedException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void 이미_취소된_예약_재취소_실패() {
        // Given
        CancelReservationCommand command = new CancelReservationCommand(1L, 1L);
        Reservation canceledReservation = createCanceledReservation(1L, 1L);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(canceledReservation));

        // When & Then
        assertThatThrownBy(() -> useCase.cancelReservation(command))
                .isInstanceOf(ReservationAlreadyCanceledException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void 커맨드_검증_필수_필드_누락시_예외_발생() {
        // When & Then
        assertThatThrownBy(() -> new CancelReservationCommand(null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 ID는 필수입니다");

        assertThatThrownBy(() -> new CancelReservationCommand(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 필수입니다");

        assertThatThrownBy(() -> new CancelReservationCommand(0L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 ID는 필수입니다");
    }

    // === 테스트 헬퍼 메서드들 ===

    private Reservation createPendingReservation(Long reservationId, Long userId) {
        return new Reservation(
                reservationId,
                userId,
                1L, // concertDateId
                1L, // seatId
                ReservationStatus.PENDING,
                50000L, // amount
                LocalDateTime.now().plusMinutes(5), // holdExpiresAt
                null, // confirmedAt
                null, // canceledAt
                0L // version
        );
    }

    private Reservation createConfirmedReservation(Long reservationId, Long userId) {
        return new Reservation(
                reservationId,
                userId,
                1L, // concertDateId
                1L, // seatId
                ReservationStatus.CONFIRMED,
                50000L, // amount
                null, // holdExpiresAt
                LocalDateTime.now().minusMinutes(10), // confirmedAt
                null, // canceledAt
                0L // version
        );
    }

    private Reservation createCanceledReservation(Long reservationId, Long userId) {
        return new Reservation(
                reservationId,
                userId,
                1L, // concertDateId
                1L, // seatId
                ReservationStatus.CANCELED,
                50000L, // amount
                null, // holdExpiresAt
                null, // confirmedAt
                LocalDateTime.now(), // canceledAt
                0L // version
        );
    }

    private Payment createSuccessfulPayment(Long paymentId, Long reservationId) {
        return new Payment(
                paymentId,
                reservationId,
                50000L, // amount
                PaymentStatus.SUCCEEDED,
                "test-idempotency-key",
                "virtual_txn_12345678",
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().minusMinutes(10)
        );
    }
}