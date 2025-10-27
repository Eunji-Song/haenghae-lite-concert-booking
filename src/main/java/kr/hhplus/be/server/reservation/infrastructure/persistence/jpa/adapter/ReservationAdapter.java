package kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.adapter;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.reservation.PaymentAmountMismatchException;
import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.ReservationNotFoundException;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.payment.application.port.out.ReservationForPayment;
import kr.hhplus.be.server.payment.application.port.out.ReservationPort;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.repository.ReservationEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationAdapter implements ReservationPort {

    private final ReservationEntityRepository reservationRepo;
    private final UserJpaRepository userRepo;

    @Override
    public ReservationForPayment getReservationForPayment(Long reservationId, String userUuid, long amount) {
        ReservationEntity reservation = reservationRepo.findById(reservationId)
                .orElseThrow(ReservationNotFoundException::new);

        Long requesterId = userRepo.findIdByUserUuid(userUuid)
                .orElseThrow(ReservationAccessDeniedException::new);

        if (!reservation.getUser().getId().equals(requesterId)) {
            throw new ReservationAccessDeniedException();
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Reservation is not PENDING");
        }
        if (reservation.getAmount() != amount) {
            throw new PaymentAmountMismatchException();
        }

        return new ReservationForPayment(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getConcert().getId(),
                reservation.getAmount()
        );
    }

    @Override
    @Transactional
    public void confirm(Long reservationId, String userUuid) {
        ReservationEntity reservation = reservationRepo.findById(reservationId)
                .orElseThrow(ReservationNotFoundException::new);

        Long requesterId = userRepo.findIdByUserUuid(userUuid)
                .orElseThrow(ReservationAccessDeniedException::new);

        if (!reservation.getUser().getId().equals(requesterId)) {
            throw new ReservationAccessDeniedException();
        }
        reservation.confirm(LocalDateTime.now());
    }
}