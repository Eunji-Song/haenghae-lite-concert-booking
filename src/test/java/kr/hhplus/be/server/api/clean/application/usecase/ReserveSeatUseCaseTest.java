package kr.hhplus.be.server.api.clean.application.usecase;

import kr.hhplus.be.server.api.clean.application.port.in.reservation.ReservationResponse;
import kr.hhplus.be.server.api.clean.application.port.in.reservation.ReserveSeatCommand;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ConcertSeatRepository;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.clean.domain.model.reservation.Reservation;
import kr.hhplus.be.server.common.entity.concert.ConcertSeatEntity;
import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.concert.SeatNotFoundException;
import kr.hhplus.be.server.common.exception.reservation.SeatAlreadyReservedException;
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
@DisplayName("ReserveSeatUseCase 테스트")
class ReserveSeatUseCaseTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ConcertSeatRepository concertSeatRepository;

    @InjectMocks
    private ReserveSeatUseCase useCase;

    @Test
    void 좌석_예약_성공() {
        // Given
        ReserveSeatCommand command = new ReserveSeatCommand(1L, 1L, "2025-09-10", 12L);
        ConcertSeatEntity mockSeat = createMockSeat(1L, 50000L);
        Reservation savedReservation = createMockReservation(1L);

        when(concertSeatRepository.findByConcertDateAndSeatNo(any(), any()))
                .thenReturn(Optional.of(mockSeat));
        when(reservationRepository.existsActiveBySeat(any())).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        // When
        ReservationResponse result = useCase.reserveSeat(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.reservationId()).isEqualTo(1L);
        assertThat(result.holdExpiresAt()).isAfter(LocalDateTime.now());

        verify(concertSeatRepository).findByConcertDateAndSeatNo(any(), eq(12L));
        verify(reservationRepository).existsActiveBySeat(mockSeat.getId());
        verify(reservationRepository).save(argThat(reservation ->
                reservation.getUserId().equals(1L) &&
                        reservation.getSeatId().equals(mockSeat.getId()) &&
                        reservation.getStatus() == ReservationStatus.PENDING
        ));
    }

    @Test
    void 미존재_좌석_선택_예약_불가() {
        // Given
        ReserveSeatCommand command = new ReserveSeatCommand(1L, 1L, "2025-09-10", 99L);

        when(concertSeatRepository.findByConcertDateAndSeatNo(any(), any()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.reserveSeat(command))
                .isInstanceOf(SeatNotFoundException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void 이미_예약이_완료된_좌석으로_예약_불가능() {
        // Given
        ReserveSeatCommand command = new ReserveSeatCommand(1L, 1L, "2025-09-10", 12L);
        ConcertSeatEntity realSeat = createRealSeat(1L, 50000L);

        when(concertSeatRepository.findByConcertDateAndSeatNo(any(), any()))
                .thenReturn(Optional.of(realSeat));
        when(reservationRepository.existsActiveBySeat(any())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> useCase.reserveSeat(command))
                .isInstanceOf(SeatAlreadyReservedException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void 날짜_형식_입력_오류() {
        // Given
        ReserveSeatCommand command = new ReserveSeatCommand(1L, 1L, "2025-13-45", 12L);

        // When & Then
        assertThatThrownBy(() -> useCase.reserveSeat(command))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 예약관련_필수입력_데이터_누락() {
        // When & Then
        assertThatThrownBy(() -> new ReserveSeatCommand(null, 1L, "2025-09-10", 12L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 필수입니다");

        assertThatThrownBy(() -> new ReserveSeatCommand(1L, null, "2025-09-10", 12L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("콘서트 ID는 필수입니다");

        assertThatThrownBy(() -> new ReserveSeatCommand(1L, 1L, null, 12L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("날짜는 필수입니다");

        assertThatThrownBy(() -> new ReserveSeatCommand(1L, 1L, "2025-09-10", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("좌석 번호는 필수입니다");
    }

    // === 테스트 헬퍼 메서드들 ===

    private ConcertSeatEntity createMockSeat(Long seatId, Long price) {
        ConcertSeatEntity seat = mock(ConcertSeatEntity.class);
        when(seat.getId()).thenReturn(seatId);
        when(seat.getPrice()).thenReturn(price);
        return seat;
    }

    private Reservation createMockReservation(Long reservationId) {
        return new Reservation(
                reservationId,
                1L, // userId
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

    private ConcertSeatEntity createRealSeat(Long seatId, Long price) {
        return ConcertSeatEntity.builder()
                .id(seatId)
                .seatNo(12L)
                .price(price)
                .build();
    }
}