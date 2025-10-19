package kr.hhplus.be.server.api.clean.application.usecase;

import kr.hhplus.be.server.api.clean.application.port.in.payment.PayForReservationCommand;
import kr.hhplus.be.server.api.clean.application.port.in.payment.PaymentResponse;
import kr.hhplus.be.server.api.clean.application.port.out.payment.PaymentRepository;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.clean.domain.model.payment.Payment;
import kr.hhplus.be.server.api.clean.domain.model.reservation.Reservation;
import kr.hhplus.be.server.common.enums.PaymentStatus;
import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.reservation.InvalidReservationStatusException;
import kr.hhplus.be.server.common.exception.reservation.PaymentAmountMismatchException;
import kr.hhplus.be.server.common.exception.reservation.ReservationHoldExpiredException;
import kr.hhplus.be.server.common.exception.reservation.ReservationNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PayForReservationUseCase 테스트")
class PayForReservationUseCaseTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PayForReservationUseCase useCase;

    @Test
    void 유효한_예약_결제_성공() {
        // Given
        PayForReservationCommand command = new PayForReservationCommand(1L, 50000L, "idem-key");
        Reservation validReservation = createValidPendingReservation();
        Payment savedPayment = createMockPayment(1L);
        Reservation confirmedReservation = createConfirmedReservation();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(validReservation));
        when(paymentRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(confirmedReservation);

        // When
        PaymentResponse result = useCase.payForReservation(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.paymentId()).isEqualTo(1L);
        assertThat(result.reservationId()).isEqualTo(1L);
        assertThat(result.confirmed()).isTrue();

        verify(reservationRepository).findById(1L);
        verify(paymentRepository).save(argThat(payment ->
                payment.getReservationId().equals(1L) &&
                        payment.getAmount().equals(50000L) &&
                        payment.getStatus() == PaymentStatus.SUCCEEDED
        ));
        verify(reservationRepository).save(argThat(reservation ->
                reservation.getStatus() == ReservationStatus.CONFIRMED
        ));
    }

    @Test
    void 존재하지_않는_예약_결제_실패() {
        // Given
        PayForReservationCommand command = new PayForReservationCommand(999L, 50000L, "idem-key");

        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.payForReservation(command))
                .isInstanceOf(ReservationNotFoundException.class);

        verify(paymentRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void 만료된_예약_결제_실패() {
        // Given
        PayForReservationCommand command = new PayForReservationCommand(1L, 50000L, "idem-key");
        Reservation expiredReservation = createExpiredReservation();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(expiredReservation));

        // When & Then
        assertThatThrownBy(() -> useCase.payForReservation(command))
                .isInstanceOf(ReservationHoldExpiredException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void 결제_금액_불일치_실패() {
        // Given
        PayForReservationCommand command = new PayForReservationCommand(1L, 30000L,"idem-key"); // 다른 금액
        Reservation validReservation = createValidPendingReservation(); // 50000L

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(validReservation));

        // When & Then
        assertThatThrownBy(() -> useCase.payForReservation(command))
                .isInstanceOf(PaymentAmountMismatchException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void 이미_확정된_예약건은_결제_불가능() {
        // Given
        PayForReservationCommand command = new PayForReservationCommand(1L, 50000L, "idem-key");
        Reservation confirmedReservation = createConfirmedReservation();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(confirmedReservation));

        // When & Then
        assertThatThrownBy(() -> useCase.payForReservation(command))
                .isInstanceOf(InvalidReservationStatusException.class)
                .hasMessage("결제 가능한 상태가 아닙니다");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void 취소된_예약_결제_실패() {
        // Given
        PayForReservationCommand command = new PayForReservationCommand(1L, 50000L, "idem-key");
        Reservation canceledReservation = createCanceledReservation();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(canceledReservation));

        // When & Then
        assertThatThrownBy(() -> useCase.payForReservation(command))
                .isInstanceOf(InvalidReservationStatusException.class)
                .hasMessage("결제 가능한 상태가 아닙니다");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void 중복_결제_요청_실패() {
        // Given
        PayForReservationCommand command = new PayForReservationCommand(1L, 50000L, "idem-key");
        Reservation validReservation = createValidPendingReservation();
        Payment existingPayment = createMockPayment(2L);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(validReservation));
        when(paymentRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.of(existingPayment));

        // When & Then
        assertThatThrownBy(() -> useCase.payForReservation(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 처리된 결제 요청입니다");

        verify(paymentRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void 커맨드_검증_필수_필드_누락시_예외_발생() {
        // When & Then

        assertThatThrownBy(() -> new PayForReservationCommand(null, 50000L, "idem-key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 ID는 필수입니다");
        assertThatThrownBy(() -> new PayForReservationCommand(1L, null, "idem-key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("결제 금액은 0보다 커야 합니다");

        assertThatThrownBy(() -> new PayForReservationCommand(1L, 0L, "idem-key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("결제 금액은 0보다 커야 합니다");
    }

    // === 테스트 헬퍼 메서드들 ===

    private Reservation createValidPendingReservation() {
        return new Reservation(
                1L, // id
                1L, // userId
                1L, // concertDateId
                1L, // seatId
                ReservationStatus.PENDING,
                50000L, // amount
                LocalDateTime.now().plusMinutes(5), // holdExpiresAt (아직 만료 안됨)
                null, // confirmedAt
                null, // canceledAt
                0L // version
        );
    }

    private Reservation createExpiredReservation() {
        return new Reservation(
                1L, // id
                1L, // userId
                1L, // concertDateId
                1L, // seatId
                ReservationStatus.PENDING,
                50000L, // amount
                LocalDateTime.now().minusMinutes(10), // holdExpiresAt (만료됨)
                null, // confirmedAt
                null, // canceledAt
                0L // version
        );
    }

    private Reservation createConfirmedReservation() {
        return new Reservation(
                1L, // id
                1L, // userId
                1L, // concertDateId
                1L, // seatId
                ReservationStatus.CONFIRMED,
                50000L, // amount
                null, // holdExpiresAt
                LocalDateTime.now(), // confirmedAt
                null, // canceledAt
                0L // version
        );
    }

    private Reservation createCanceledReservation() {
        return new Reservation(
                1L, // id
                1L, // userId
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

    private Payment createMockPayment(Long paymentId) {
        return new Payment(
                paymentId,
                1L, // reservationId
                50000L, // amount
                PaymentStatus.SUCCEEDED,
                "test-idempotency-key",
                "virtual_txn_12345678",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}