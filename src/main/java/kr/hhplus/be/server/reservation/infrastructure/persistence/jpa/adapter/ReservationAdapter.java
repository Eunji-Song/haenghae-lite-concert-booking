package kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.adapter;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.reservation.PaymentAmountMismatchException;
import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.ReservationNotFoundException;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.payment.application.port.out.ReservationForPayment;
import kr.hhplus.be.server.payment.application.port.out.ReservationPort;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.repository.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationAdapter implements ReservationPort {

    private final ReservationJpaRepository reservationRepo;
    private final UserJpaRepository userRepo;

    @Override
    public ReservationForPayment getReservationForPayment(Long reservationId, String userUuid) {
        var reservation = reservationRepo.findById(reservationId)
                .orElseThrow(ReservationNotFoundException::new);

        var requesterId = userRepo.findIdByUserUuid(userUuid)
                .orElseThrow(ReservationAccessDeniedException::new);

        if (!reservation.getUserId().equals(requesterId)) throw new ReservationAccessDeniedException();
        if (reservation.getStatus() != ReservationStatus.PENDING) throw new IllegalStateException("Reservation is not PENDING");

        return new ReservationForPayment(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getConcertId(),
                reservation.getAmount()
        );
    }

    @Override
    @Transactional
    public void confirm(Long reservationId, String userUuid) {
        // 1) 예약 로드
        ReservationEntity reservation = reservationRepo.findById(reservationId)
                .orElseThrow(ReservationNotFoundException::new);

        // 2) 요청자 ID 조회
        Long requesterId = userRepo.findIdByUserUuid(userUuid)
                .orElseThrow(ReservationAccessDeniedException::new);

        // 3) 소유자 검증은 FK 필드로!
        if (!reservation.getUserId().equals(requesterId)) {
            throw new ReservationAccessDeniedException();
        }

        // 4) 상태 검증
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Reservation is not PENDING");
        }

        // 5) 상태 전이 + 저장 (낙관적 락은 @Version으로 동작)
        reservation.confirm(LocalDateTime.now());
        reservationRepo.save(reservation);
    }
}