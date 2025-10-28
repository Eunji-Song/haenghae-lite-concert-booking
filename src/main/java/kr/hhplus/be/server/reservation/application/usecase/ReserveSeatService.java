package kr.hhplus.be.server.reservation.application.usecase;

import kr.hhplus.be.server.common.enums.ReservationStatus;
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

        // 사용자 검증
        var userId = userRepo.findIdByUserUuid(cmd.userUuid())
                .orElseThrow(ReservationAccessDeniedException::new);

        // 공연 날짜 ID 조회 (DateResolver 사용)
        Long dateId = dateResolver.resolveDateId(cmd.concertId(), cmd.date());

        //  좌석 ID 조회
        Long seatId = reservationRepository.resolveSeatId(cmd.concertId(), cmd.date(), cmd.seatNo());

        // 가격
        long amount = reservationRepository.findSeatPriceBySeatId(seatId);

        // 홀드 만료 시간
        LocalDateTime holdUntil = LocalDateTime.now(clock).plusSeconds(cmd.holdSeconds());


        // 좌석 점유 가능 여부 확인
        if (!reservationRepository.isSeatOccupiable(seatId)) {
            throw new SeatAlreadyReservedException();
        }

        // 예약 생성 (PENDING 상태)
        var pending = new Reservation(
                null,
                userId,
                cmd.concertId(),
                dateId,
                seatId,
                ReservationStatus.PENDING,
                amount,
                holdUntil,
                null, null, null, null, null, null
        );

        var saved = reservationRepository.save(pending);

        return new ReservationResponse(saved.getId(), saved.getHoldExpiresAt());
    }
}