package kr.hhplus.be.server.api.clean.application.usecase;

import kr.hhplus.be.server.api.clean.application.port.in.reservation.GetReservationQuery;
import kr.hhplus.be.server.api.clean.application.port.in.reservation.ReservationDetailResponse;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.clean.domain.model.reservation.Reservation;
import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.ReservationNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetReservationUseCase 테스트")
class GetReservationUseCaseTest {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private GetReservationUseCase useCase;

    @Test
    @DisplayName("본인의 예약 상세 조회가 성공한다")
    void 본인_예약_상세_조회_성공() {
        // Given
        GetReservationQuery query = new GetReservationQuery(1L, 1L);
        Reservation reservation = createConfirmedReservation(1L, 1L);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        // When
        ReservationDetailResponse result = useCase.getReservation(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.reservationId()).isEqualTo(1L);
        assertThat(result.concertId()).isEqualTo(1L);
        assertThat(result.date()).isEqualTo("2025-09-10");
        assertThat(result.seatNo()).isEqualTo(12L);
        assertThat(result.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(result.paidAmount()).isEqualTo(50000L);
        

        verify(reservationRepository).findById(1L);
    }

    @Test
    @DisplayName("PENDING 상태의 예약 조회 시 홀드 만료 시간이 포함된다")
    void PENDING_상태_예약_조회_홀드시간_포함() {
        // Given
        GetReservationQuery query = new GetReservationQuery(1L, 1L);
        Reservation pendingReservation = createPendingReservation(1L, 1L);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));

        // When
        ReservationDetailResponse result = useCase.getReservation(query);

        // Then
        assertThat(result.status()).isEqualTo(ReservationStatus.PENDING);
        assertThat(result.holdExpiresAt()).isNotNull();
        assertThat(result.holdExpiresAt()).isAfter(LocalDateTime.now());

        verify(reservationRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 예약 조회 시 예외 발생")
    void 존재하지_않는_예약_조회_실패() {
        // Given
        GetReservationQuery query = new GetReservationQuery(999L, 1L);

        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.getReservation(query))
                .isInstanceOf(ReservationNotFoundException.class);

        verify(reservationRepository).findById(999L);
    }

    @Test
    @DisplayName("다른 사용자의 예약 조회 시 예외 발생")
    void 다른_사용자_예약_조회_권한_없음() {
        // Given
        GetReservationQuery query = new GetReservationQuery(1L, 2L); // userId=2L로 시도
        Reservation otherUserReservation = createConfirmedReservation(1L, 1L); // userId=1L의 예약

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(otherUserReservation));

        // When & Then
        assertThatThrownBy(() -> useCase.getReservation(query))
                .isInstanceOf(ReservationAccessDeniedException.class);

        verify(reservationRepository).findById(1L);
    }

    @Test
    @DisplayName("취소된 예약도 조회 가능하다")
    void 취소된_예약_조회_가능() {
        // Given
        GetReservationQuery query = new GetReservationQuery(1L, 1L);
        Reservation canceledReservation = createCanceledReservation(1L, 1L);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(canceledReservation));

        // When
        ReservationDetailResponse result = useCase.getReservation(query);

        // Then
        assertThat(result.status()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(result.holdExpiresAt()).isNull(); // 취소되면 홀드 시간 없음

        verify(reservationRepository).findById(1L);
    }

    @Test
    @DisplayName("Query 검증 - 필수 필드 누락시 예외 발생")
    void 쿼리_검증_필수_필드_누락시_예외_발생() {
        // When & Then
        assertThatThrownBy(() -> new GetReservationQuery(null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 ID는 필수입니다");

        assertThatThrownBy(() -> new GetReservationQuery(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 필수입니다");

        assertThatThrownBy(() -> new GetReservationQuery(0L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 ID는 필수입니다");
    }

    // === 테스트 헬퍼 메서드들 ===

    private Reservation createConfirmedReservation(Long reservationId, Long userId) {
        // Reservation 생성자에 createdAt, updatedAt 파라미터가 필요한지 확인
        // 임시로 createPendingReservation 사용 후 상태 변경
        // Reservation reservation = Reservation.createPendingReservation(userId, 1L, 1L, 50000L);

        Reservation reservation = new Reservation(
                reservationId,
                userId,
                1L, // concertDateId
                1L, // seatId
                ReservationStatus.CONFIRMED,
                50000L, // amount
                LocalDateTime.now().plusMinutes(5), // holdExpiresAt
                LocalDateTime.now(), // confirmedAt
                null, // canceledAt
                0L // version
        );

        return reservation;
    }

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
                LocalDateTime.now().minusMinutes(5), // canceledAt
                0L // version
        );
    }
}