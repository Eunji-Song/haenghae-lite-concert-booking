package kr.hhplus.be.server.reservation.application.usecase;

import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.SeatAlreadyReservedException;
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
@Transactional(readOnly = true)
public class ReserveSeatService implements ReserveSeatUseCase {

    private final ReservationRepository reservationRepository;
    private final UserJpaRepository userRepo;
    private final DateResolver dateResolver;
    private final Clock clock;

    @Override
    @Transactional
    public ReservationResponse reserve(ReserveSeatCommand cmd) {
        Long userId = userRepo.findIdByUserUuid(cmd.userUuid())
                .orElseThrow(ReservationAccessDeniedException::new);

        Long dateId = dateResolver.resolveDateId(cmd.concertId(), cmd.date());
        Long seatId = reservationRepository.resolveSeatId(cmd.concertId(), cmd.date(), cmd.seatNo());

        // 1) 좌석 행 잠금
        reservationRepository.lockSeat(seatId);

        // 2) 잠금 획득 후 점유 가능 재확인
        if (!reservationRepository.isSeatOccupiable(seatId)) {
            throw new SeatAlreadyReservedException();
        }

        long amount = reservationRepository.findSeatPriceBySeatId(seatId);
        LocalDateTime holdUntil = LocalDateTime.now(clock).plusSeconds(cmd.holdSeconds());

        // 3) 활성 홀드로 생성 (isActive=true)
        Reservation pending = Reservation.pending(userId, cmd.concertId(), dateId, seatId, amount, holdUntil);

        var saved = reservationRepository.save(pending); // 유니크 위반 시 SeatAlreadyReservedException 변환
        return new ReservationResponse(saved.getId(), saved.getHoldExpiresAt());
    }
}