package kr.hhplus.be.server.reservation.application.usecase;

import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.ReservationNotFoundException;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.reservation.application.port.in.usecase.CancelReservationUseCase;
import kr.hhplus.be.server.reservation.application.port.out.ReservationRepository;
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
        // 사용자 검증
        Long userId = userRepo.findIdByUserUuid(userUuid)
                .orElseThrow(ReservationAccessDeniedException::new);

        // 예약 조회
        var reservation = reservationRepository.findById(reservationId)
                .orElseThrow(ReservationNotFoundException::new);

        // 본인 예약인지 확인
        if (!reservation.getUserId().equals(userId)) {
            throw new ReservationAccessDeniedException();
        }

        // 도메인 로직으로 상태 변경
        var canceled = reservation.cancel(LocalDateTime.now(clock));

        // 변경된 상태 저장
        reservationRepository.save(canceled);
    }
}