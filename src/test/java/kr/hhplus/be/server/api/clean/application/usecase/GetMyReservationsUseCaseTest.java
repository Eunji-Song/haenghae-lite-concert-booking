package kr.hhplus.be.server.api.clean.application.usecase;

import kr.hhplus.be.server.api.clean.application.port.in.reservation.GetMyReservationsQuery;
import kr.hhplus.be.server.api.clean.application.port.in.reservation.ReservationSummaryResponse;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.clean.domain.model.reservation.Reservation;
import kr.hhplus.be.server.common.enums.ReservationStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMyReservationsUseCase 테스트")
class GetMyReservationsUseCaseTest {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private GetMyReservationsUseCase useCase;

    @Test
    @DisplayName("사용자의 예약 목록을 성공적으로 조회한다")
    void 사용자_예약_목록_조회_성공() {
        // Given
        GetMyReservationsQuery query = new GetMyReservationsQuery(1L);
        List<Reservation> reservations = Arrays.asList(
                createReservationWithTimeAndStatus(1L, 1L, LocalDateTime.now(), ReservationStatus.CONFIRMED),
                createReservationWithTimeAndStatus(2L, 1L, LocalDateTime.now().minusMinutes(1), ReservationStatus.PENDING),
                createReservationWithTimeAndStatus(3L, 1L, LocalDateTime.now().minusMinutes(10), ReservationStatus.CANCELED)
        );

        when(reservationRepository.findByUserId(1L)).thenReturn(reservations);

        // When
        List<ReservationSummaryResponse> result = useCase.getMyReservations(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        // 첫 번째 예약 (CONFIRMED)
        ReservationSummaryResponse first = result.get(0);
        assertThat(first.reservationId()).isEqualTo(1L);
        assertThat(first.concertId()).isEqualTo(1L);
        assertThat(first.date()).isEqualTo("2025-09-10");
        assertThat(first.seatNo()).isEqualTo(12L);
        assertThat(first.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(first.paidAmount()).isEqualTo(50000L);

        // 두 번째 예약 (PENDING)
        ReservationSummaryResponse second = result.get(1);
        assertThat(second.status()).isEqualTo(ReservationStatus.PENDING);

        // 세 번째 예약 (CANCELED)
        ReservationSummaryResponse third = result.get(2);
        assertThat(third.status()).isEqualTo(ReservationStatus.CANCELED);

        verify(reservationRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("예약이 없는 사용자는 빈 목록을 반환한다")
    void 예약_없는_사용자_빈_목록_반환() {
        // Given
        GetMyReservationsQuery query = new GetMyReservationsQuery(99L);

        when(reservationRepository.findByUserId(99L)).thenReturn(Collections.emptyList());

        // When
        List<ReservationSummaryResponse> result = useCase.getMyReservations(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(reservationRepository).findByUserId(99L);
    }

    @Test
    @DisplayName("예약 목록이 최신순으로 정렬된다")
    void 예약_목록_최신순_정렬() {
        // Given
        GetMyReservationsQuery query = new GetMyReservationsQuery(1L);

        // 생성 시간이 다른 예약들 (오래된 것부터)
        Reservation oldReservation = createReservationWithTime(1L, 1L, LocalDateTime.now().minusDays(3));
        Reservation middleReservation = createReservationWithTime(2L, 1L, LocalDateTime.now().minusDays(1));
        Reservation newReservation = createReservationWithTime(3L, 1L, LocalDateTime.now());

        // Repository에서는 시간순 관계없이 반환 (정렬은 UseCase에서)
        List<Reservation> reservations = Arrays.asList(oldReservation, newReservation, middleReservation);

        when(reservationRepository.findByUserId(1L)).thenReturn(reservations);

        // When
        List<ReservationSummaryResponse> result = useCase.getMyReservations(query);

        // Then
        assertThat(result).hasSize(3);
        // 최신순으로 정렬되었는지 확인 (가장 최근이 첫 번째)
        assertThat(result.get(0).reservationId()).isEqualTo(3L); // 가장 최근
        assertThat(result.get(1).reservationId()).isEqualTo(2L); // 중간
        assertThat(result.get(2).reservationId()).isEqualTo(1L); // 가장 오래됨

        verify(reservationRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("Query 검증 - 필수 필드 누락시 예외 발생")
    void 쿼리_검증_필수_필드_누락시_예외_발생() {
        // When & Then
        assertThatThrownBy(() -> new GetMyReservationsQuery(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 필수입니다");

        assertThatThrownBy(() -> new GetMyReservationsQuery(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 필수입니다");
    }

    // === 테스트 헬퍼 메서드들 ===

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
                0L, // version,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
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
                0L,  // version
                LocalDateTime.now(),
                LocalDateTime.now()
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
                0L, // version
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private Reservation createReservationWithTime(Long reservationId, Long userId, LocalDateTime createdTime) {
        // 생성 시간을 지정할 수 있는 헬퍼 메서드
        // 실제로는 Reservation에 createdAt 필드가 있어야 함
        return new Reservation(
                reservationId,
                userId,
                1L, // concertDateId
                1L, // seatId
                ReservationStatus.CONFIRMED,
                50000L, // amount
                null, // holdExpiresAt
                createdTime, // confirmedAt을 createdTime으로 사용
                null, // canceledAt
                0L,  // version
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private Reservation createReservationWithTimeAndStatus(Long reservationId, Long userId, LocalDateTime createdTime, ReservationStatus status) {
        // 생성 시간을 지정할 수 있는 헬퍼 메서드
        // 실제로는 Reservation에 createdAt 필드가 있어야 함
        return new Reservation(
                reservationId,
                userId,
                1L, // concertDateId
                1L, // seatId
                status,
                50000L, // amount
                null, // holdExpiresAt
                createdTime, // confirmedAt을 createdTime으로 사용
                null, // canceledAt
                0L,  // version
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}