package kr.hhplus.be.server.reservation.application.usecase;

import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.SeatAlreadyReservedException;
import kr.hhplus.be.server.common.lock.DistributedLockExecutor;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.reservation.application.port.in.command.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.port.in.result.ReservationResponse;
import kr.hhplus.be.server.reservation.application.port.in.usecase.ReserveSeatUseCase;
import kr.hhplus.be.server.reservation.application.port.out.ReservationRepository;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.support.DateResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReserveSeatService implements ReserveSeatUseCase {

    private final ReservationRepository reservationRepository;
    private final UserJpaRepository userRepo;
    private final DateResolver dateResolver;
    private final Clock clock;
    private final DistributedLockExecutor lockExecutor;

    @Override
    public ReservationResponse reserve(ReserveSeatCommand cmd) {

        String lockKey = buildLockKey(cmd);

        // Redis 분산락 + DB 트랜잭션 함께 적용
        return lockExecutor.executeWithLock(
                lockKey,
                3_000,  // 3초 대기 + TTL
                () -> performReservation(cmd)   // 내부 트랜잭션 시작
        );
    }

    @Transactional
    public ReservationResponse performReservation(ReserveSeatCommand cmd) {
        Long userId = userRepo.findIdByUserUuid(cmd.userUuid())
                .orElseThrow(ReservationAccessDeniedException::new);

        Long dateId = dateResolver.resolveDateId(cmd.concertId(), cmd.date());
        Long seatId = reservationRepository.resolveSeatId(cmd.concertId(), cmd.date(), cmd.seatNo());

        // DB 레벨 락도 병행 (pessimistic write)
        reservationRepository.lockSeat(seatId);

        if (!reservationRepository.isSeatOccupiable(seatId)) {
            throw new SeatAlreadyReservedException();
        }

        long amount = reservationRepository.findSeatPriceBySeatId(seatId);
        LocalDateTime holdUntil = LocalDateTime.now(clock).plusSeconds(cmd.holdSeconds());

        Reservation pending = Reservation.pending(
                userId, cmd.concertId(), dateId, seatId, amount, holdUntil
        );

        var saved = reservationRepository.save(pending);
        return new ReservationResponse(saved.getId(), saved.getHoldExpiresAt());
    }

    private String buildLockKey(ReserveSeatCommand cmd) {
        return "reservation:seat:" + cmd.concertId()
                + ":" + cmd.date()
                + ":" + cmd.seatNo();
    }
}