package kr.hhplus.be.server.reservation.application.usecase;

import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.SeatAlreadyReservedException;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.reservation.application.port.in.command.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.port.in.result.ReservationResponse;
import kr.hhplus.be.server.reservation.application.port.out.ReservationRepository;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.support.DateResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReserveSeatServiceTest {

    private ReservationRepository reservationRepository;
    private UserJpaRepository userRepo;
    private DateResolver dateResolver;
    private Clock clock;

    private ReserveSeatService sut;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        userRepo = mock(UserJpaRepository.class);
        dateResolver = mock(DateResolver.class);
        clock = Clock.fixed(Instant.parse("2025-10-28T00:00:00Z"), ZoneId.of("UTC"));

        sut = new ReserveSeatService(reservationRepository, userRepo, dateResolver, clock);
    }

    @Test
    void reserve_success() {
        // given
        String userUuid = "u-uuid";
        Long userId = 10L;
        Long concertId = 101L;
        LocalDate date = LocalDate.of(2025, 11, 1);
        int seatNo = 12;
        long holdSeconds = 600L;

        when(userRepo.findIdByUserUuid(userUuid)).thenReturn(java.util.Optional.of(userId));
        when(dateResolver.resolveDateId(concertId, date)).thenReturn(1001L);
        when(reservationRepository.resolveSeatId(concertId, date, seatNo)).thenReturn(5001L);
        when(reservationRepository.isSeatOccupiable(5001L)).thenReturn(true);

        // save 시점에 저장된 Reservation 을 id 포함으로 리턴
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            return new Reservation(
                    9001L,
                    r.getUserId(),
                    r.getConcertId(),
                    r.getConcertDateId(),
                    r.getSeatId(),
                    r.getStatus(),
                    r.getAmount(),
                    r.getHoldExpiresAt(),
                    r.getConfirmedAt(),
                    r.getCanceledAt(),
                    r.getExpiredAt(),
                    true,
                    null,
                    r.getCreatedAt(),
                    r.getUpdatedAt()
            );
        });

        var cmd = new ReserveSeatCommand(userUuid, concertId, date, seatNo, holdSeconds);

        // when
        ReservationResponse res = sut.reserve(cmd);

        // then
        assertThat(res.reservationId()).isEqualTo(9001L);
        assertThat(res.holdExpiresAt()).isEqualTo(LocalDateTime.ofInstant(Instant.parse("2025-10-28T00:00:00Z"), ZoneOffset.UTC).plusSeconds(holdSeconds));
        verify(reservationRepository).isSeatOccupiable(5001L);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void reserve_fail_when_user_not_found() {
        // given
        when(userRepo.findIdByUserUuid("missing")).thenReturn(java.util.Optional.empty());
        var cmd = new ReserveSeatCommand("missing", 1L, LocalDate.now(), 1, 300);

        // expect
        assertThatThrownBy(() -> sut.reserve(cmd))
                .isInstanceOf(ReservationAccessDeniedException.class);
    }

    @Test
    void reserve_fail_when_seat_already_reserved() {
        // given
        String userUuid = "u-uuid";
        Long userId = 10L;
        when(userRepo.findIdByUserUuid(userUuid)).thenReturn(java.util.Optional.of(userId));
        when(dateResolver.resolveDateId(anyLong(), any(LocalDate.class))).thenReturn(1001L);
        when(reservationRepository.resolveSeatId(anyLong(), any(LocalDate.class), anyInt())).thenReturn(5001L);
        when(reservationRepository.isSeatOccupiable(5001L)).thenReturn(false);

        var cmd = new ReserveSeatCommand(userUuid, 1L, LocalDate.now(), 5, 300);

        // expect
        assertThatThrownBy(() -> sut.reserve(cmd))
                .isInstanceOf(SeatAlreadyReservedException.class);
    }
}