package kr.hhplus.be.server.reservation.application.usecase;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.ReservationNotFoundException;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.reservation.application.port.out.ReservationRepository;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CancelReservationService 단위 테스트
 */
class CancelReservationServiceTest {

    private ReservationRepository reservationRepository;
    private UserJpaRepository userRepo;
    private Clock clock;

    private CancelReservationService sut;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        userRepo = mock(UserJpaRepository.class);
        clock = Clock.fixed(Instant.parse("2025-10-28T00:00:00Z"), ZoneId.of("UTC"));
        sut = new CancelReservationService(reservationRepository, userRepo, clock);
    }

    @Test
    void cancel_success() {
        // given
        String userUuid = "u-uuid";
        Long userId = 11L;
        Long reservationId = 7001L;

        when(userRepo.findIdByUserUuid(userUuid)).thenReturn(Optional.of(userId));

        var existing = Reservation.pending(
                userId,
                101L,     // concertId
                1001L,    // concertDateId
                5001L,    // seatId
                50_000L,  // amount
                LocalDateTime.now(clock).plusMinutes(10)
        );

        // 테스트를 위해 ID와 createdAt, updatedAt을 강제로 세팅
        var existingWithId = new Reservation(
                reservationId,
                existing.getUserId(),
                existing.getConcertId(),
                existing.getConcertDateId(),
                existing.getSeatId(),
                existing.getStatus(),
                existing.getAmount(),
                existing.getHoldExpiresAt(),
                existing.getConfirmedAt(),
                existing.getCanceledAt(),
                existing.getExpiredAt(),
                true,
                existing.getVersion(),
                LocalDateTime.now(clock).minusMinutes(1),
                LocalDateTime.now(clock).minusMinutes(1)
        );

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(existingWithId));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        sut.cancel(userUuid, reservationId);

        // then
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        Reservation saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(saved.getCanceledAt()).isEqualTo(LocalDateTime.ofInstant(Instant.parse("2025-10-28T00:00:00Z"), ZoneOffset.UTC));
        assertThat(saved.getId()).isEqualTo(reservationId);
    }

    @Test
    void cancel_fail_when_user_not_found() {
        when(userRepo.findIdByUserUuid("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> sut.cancel("missing", 1L))
                .isInstanceOf(ReservationAccessDeniedException.class);
    }

    @Test
    void cancel_fail_when_reservation_not_found() {
        when(userRepo.findIdByUserUuid("u")).thenReturn(Optional.of(1L));
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.cancel("u", 999L))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void cancel_fail_when_not_owner() {
        when(userRepo.findIdByUserUuid("u1")).thenReturn(Optional.of(1L));

        var otherOwnerRes = Reservation.pending(
                999L,  // 다른 userId
                10L,   // concertId
                20L,   // concertDateId
                30L,   // seatId
                1000L, // amount
                LocalDateTime.now(clock).plusMinutes(5)
        );

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(otherOwnerRes));

        assertThatThrownBy(() -> sut.cancel("u1", 1L))
                .isInstanceOf(ReservationAccessDeniedException.class);
    }
}