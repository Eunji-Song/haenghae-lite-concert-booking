package kr.hhplus.be.server.reservation.application.usecase;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.ReservationNotFoundException;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.reservation.application.port.in.usecase.CancelReservationUseCase;
import kr.hhplus.be.server.reservation.application.port.out.ReservationRepository;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 예약 취소 서비스 (유스케이스 구현)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CancelReservationService implements CancelReservationUseCase {

    private final ReservationRepository reservationRepository;
    private final UserJpaRepository userRepo;
    private final Clock clock;

    @Override
    public void cancel(String userUuid, Long reservationId) {
        // 사용자 ID 검증
        var userId = userRepo.findIdByUserUuid(userUuid)
                .orElseThrow(ReservationAccessDeniedException::new);

        // 예약 조회
        var reservation = reservationRepository.findById(reservationId)
                .orElseThrow(ReservationNotFoundException::new);

        // 본인 예약인지 확인
        if (!reservation.getUserId().equals(userId)) {
            throw new ReservationAccessDeniedException();
        }

        // 상태 업데이트: CANCELED
        var canceled = new Reservation(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getConcertId(),
                reservation.getConcertDateId(),
                reservation.getSeatId(),
                ReservationStatus.CANCELED,
                reservation.getAmount(),
                reservation.getHoldExpiresAt(),
                reservation.getConfirmedAt(),
                LocalDateTime.now(clock),
                reservation.getExpiredAt(),
                null,
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );

        reservationRepository.save(canceled);
    }
}