package kr.hhplus.be.server.product.application.service;

import kr.hhplus.be.server.common.enums.SeatStatus;
import kr.hhplus.be.server.product.api.dto.OpenDateResponse;
import kr.hhplus.be.server.product.api.dto.SeatAvailabilityResponse;
import kr.hhplus.be.server.product.domain.model.ConcertDate;
import kr.hhplus.be.server.product.domain.model.ConcertSeat;
import kr.hhplus.be.server.product.infrastructure.jpa.adapter.ConcertDateRepositoryAdapter;
import kr.hhplus.be.server.product.infrastructure.jpa.adapter.ConcertSeatRepositoryAdapter;
import kr.hhplus.be.server.reservation.application.port.out.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * ConcertService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class ConcertServiceTest {

    @Mock private ConcertDateRepositoryAdapter dateAdapter;
    @Mock private ConcertSeatRepositoryAdapter seatAdapter;
    @Mock private ReservationRepository reservationRepository;

    @InjectMocks
    private ConcertService concertService;

    @Test
    @DisplayName("getOpenDates: 오픈된 공연 날짜를 올바르게 매핑한다")
    void getOpenDates_success() {
        // given
        Long concertId = 1L;
        var now = LocalDateTime.now();
        var d1 = new ConcertDate(10L, concertId, LocalDate.of(2025, 12, 24), "KSPO DOME", true, now, now);
        var d2 = new ConcertDate(11L, concertId, LocalDate.of(2025, 12, 25), "KSPO DOME", true, now, now);

        when(dateAdapter.getOpenDates(concertId)).thenReturn(List.of(d1, d2));

        // when
        List<OpenDateResponse> result = concertService.getOpenDates(concertId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).concertId()).isEqualTo(concertId);
        assertThat(result.get(0).concertDateId()).isEqualTo(10L);
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2025, 12, 24));
        assertThat(result.get(0).open()).isTrue();

        assertThat(result.get(1).concertDateId()).isEqualTo(11L);
        assertThat(result.get(1).date()).isEqualTo(LocalDate.of(2025, 12, 25));

        verify(dateAdapter).getOpenDates(concertId);
        verifyNoInteractions(seatAdapter, reservationRepository);
    }

    @Test
    @DisplayName("getSeats: 좌석 점유 가능 여부에 따라 AVAILABLE / HELD 상태를 반환한다")
    void getSeats_success() {
        // given
        Long concertId = 1L;
        LocalDate date = LocalDate.of(2025, 12, 31);
        var now = LocalDateTime.now();

        var seat1 = new ConcertSeat(
                100L, 10L, 1, "A", 50_000L, SeatStatus.AVAILABLE, now, now);
        var seat2 = new ConcertSeat(
                101L, 10L, 2, "A", 60_000L, SeatStatus.AVAILABLE, now, now);

        when(seatAdapter.findSeats(concertId, date)).thenReturn(List.of(seat1, seat2));
        // seat1은 예약 가능, seat2는 점유 중
        when(reservationRepository.isSeatOccupiable(100L)).thenReturn(true);
        when(reservationRepository.isSeatOccupiable(101L)).thenReturn(false);

        // when
        List<SeatAvailabilityResponse> result = concertService.getSeats(concertId, date);

        // then
        assertThat(result).hasSize(2);

        SeatAvailabilityResponse s1 = result.get(0);
        SeatAvailabilityResponse s2 = result.get(1);

        assertThat(s1.seatId()).isEqualTo(100L);
        assertThat(s1.seatNo()).isEqualTo(1);
        assertThat(s1.price()).isEqualTo(50_000L);
        assertThat(s1.status()).isEqualTo(SeatStatus.AVAILABLE);

        assertThat(s2.seatId()).isEqualTo(101L);
        assertThat(s2.seatNo()).isEqualTo(2);
        assertThat(s2.price()).isEqualTo(60_000L);
        assertThat(s2.status()).isEqualTo(SeatStatus.HELD);

        // verify interactions
        verify(seatAdapter).findSeats(concertId, date);
        verify(reservationRepository).isSeatOccupiable(100L);
        verify(reservationRepository).isSeatOccupiable(101L);
        verifyNoMoreInteractions(dateAdapter);
    }
}