package kr.hhplus.be.server.reservation.application.usecase;

import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.SeatAlreadyReservedException;
import kr.hhplus.be.server.common.lock.DistributedLockExecutor;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.reservation.application.port.in.command.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.port.in.result.ReservationResponse;
import kr.hhplus.be.server.reservation.application.port.out.ReservationRepository;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.support.DateResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReserveSeatService 단위 테스트.
 * - 분산락(DistributedLockExecutor)은 Mockito mock 으로 대체하고,
 *   executeWithLock 내부에서 실제로 Supplier/Runnable 을 실행하도록 stubbing 한다.
 */
class ReserveSeatServiceTest {

    private ReservationRepository reservationRepository;
    private UserJpaRepository userRepo;
    private DateResolver dateResolver;
    private Clock clock;
    private DistributedLockExecutor lockExecutor;

    private ReserveSeatService sut;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        userRepo = mock(UserJpaRepository.class);
        dateResolver = mock(DateResolver.class);
        clock = Clock.fixed(Instant.parse("2025-10-28T00:00:00Z"), ZoneId.of("UTC"));
        lockExecutor = mock(DistributedLockExecutor.class);

        // Supplier<T> 버전 executeWithLock 이 호출되면, 세 번째 인자로 들어온 Supplier 를 그대로 실행해서 결과를 반환하도록 설정
        when(lockExecutor.executeWithLock(anyString(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<ReservationResponse> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        // Runnable 버전은 현재 테스트에서 직접 사용하지 않지만, 혹시라도 호출되면 바로 task.run() 하도록 설정
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(2);
            task.run();
            return null;
        }).when(lockExecutor).executeWithLock(anyString(), anyLong(), any(Runnable.class));

        sut = new ReserveSeatService(
                reservationRepository,
                userRepo,
                dateResolver,
                clock,
                lockExecutor
        );
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

        when(userRepo.findIdByUserUuid(userUuid)).thenReturn(Optional.of(userId));
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
        assertThat(res.holdExpiresAt()).isEqualTo(
                LocalDateTime.ofInstant(
                        Instant.parse("2025-10-28T00:00:00Z"),
                        ZoneOffset.UTC
                ).plusSeconds(holdSeconds)
        );

        // 좌석 점유 가능 여부 확인 + 저장 호출 검증
        verify(reservationRepository).isSeatOccupiable(5001L);
        verify(reservationRepository).save(any(Reservation.class));

        // 분산락 실행이 실제로 한 번 호출되었는지도 검증 가능
        verify(lockExecutor, times(1)).executeWithLock(anyString(), anyLong(), any(Supplier.class));
    }

    @Test
    void reserve_fail_when_user_not_found() {
        // given
        when(userRepo.findIdByUserUuid("missing")).thenReturn(Optional.empty());
        when(dateResolver.resolveDateId(anyLong(), any(LocalDate.class))).thenReturn(1001L);
        when(reservationRepository.resolveSeatId(anyLong(), any(LocalDate.class), anyInt()))
                .thenReturn(5001L);
        var cmd = new ReserveSeatCommand("missing", 1L, LocalDate.now(), 1, 300);

        // expect
        assertThatThrownBy(() -> sut.reserve(cmd))
                .isInstanceOf(ReservationAccessDeniedException.class);

        // 분산락은 호출되지만, 내부에서 사용자 조회 실패로 예외가 발생함
        verify(lockExecutor, times(1)).executeWithLock(anyString(), anyLong(), any(Supplier.class));
    }

    @Test
    void reserve_fail_when_seat_already_reserved() {
        // given
        String userUuid = "u-uuid";
        Long userId = 10L;

        when(userRepo.findIdByUserUuid(userUuid)).thenReturn(Optional.of(userId));
        when(dateResolver.resolveDateId(anyLong(), any(LocalDate.class))).thenReturn(1001L);
        when(reservationRepository.resolveSeatId(anyLong(), any(LocalDate.class), anyInt()))
                .thenReturn(5001L);
        when(reservationRepository.isSeatOccupiable(5001L)).thenReturn(false);

        var cmd = new ReserveSeatCommand(userUuid, 1L, LocalDate.now(), 5, 300);

        // expect
        assertThatThrownBy(() -> sut.reserve(cmd))
                .isInstanceOf(SeatAlreadyReservedException.class);

        // 이 케이스는 lock 안에서는 정상적으로 수행되지만,
        // isSeatOccupiable false 에 의해 비즈니스 에러가 발생하는 시나리오
        verify(lockExecutor, times(1)).executeWithLock(anyString(), anyLong(), any(Supplier.class));
    }
}