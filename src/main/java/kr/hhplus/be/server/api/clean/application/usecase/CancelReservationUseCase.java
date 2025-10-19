package kr.hhplus.be.server.api.clean.application.usecase;

import kr.hhplus.be.server.api.clean.application.port.in.reservation.CancelReservationCommand;
import kr.hhplus.be.server.api.clean.application.port.in.reservation.CancelReservationResponse;
import kr.hhplus.be.server.api.clean.application.port.out.payment.PaymentRepository;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.clean.domain.model.payment.Payment;
import kr.hhplus.be.server.api.clean.domain.model.reservation.Reservation;
import kr.hhplus.be.server.common.enums.PaymentStatus;
import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.ReservationNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 예약 취소 Use Case
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CancelReservationUseCase {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    /**
     * 예약 취소 처리
     *
     * @param command 취소 요청 정보
     * @return 취소 결과
     * @throws ReservationNotFoundException 예약을 찾을 수 없는 경우
     * @throws ReservationAccessDeniedException 예약에 대한 권한이 없는 경우
     */
    @Transactional
    public CancelReservationResponse cancelReservation(CancelReservationCommand command) {
        log.info("예약 취소 요청 - reservationId: {}, userId: {}",
                command.reservationId(), command.userId());

        // 예약 존재 여부 확인
        Reservation reservation = findReservationOrThrow(command.reservationId());

        // 사용자 권한 확인
        validateUserAccess(reservation, command.userId());

        // 예약 취소 처리 (도메인 로직 활용)
        reservation.cancel();

        // 결제된 경우 환불 처리
        processRefundIfNeeded(command.reservationId());

        // 예약 상태 저장
        Reservation canceledReservation = reservationRepository.save(reservation);

        log.info("예약 취소 완료 - reservationId: {}, canceledAt: {}",
                canceledReservation.getId(), canceledReservation.getCanceledAt());

        return new CancelReservationResponse(true);
    }

    /**
     * 예약 조회 및 존재 여부 확인
     */
    private Reservation findReservationOrThrow(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }

    /**
     * 사용자 권한 확인 (본인의 예약인지)
     */
    private void validateUserAccess(Reservation reservation, Long userId) {
        if (!reservation.getUserId().equals(userId)) {
            throw new ReservationAccessDeniedException(userId, reservation.getId());
        }
    }

    /**
     * 결제된 경우 환불 처리
     */
    private void processRefundIfNeeded(Long reservationId) {
        List<Payment> payments = paymentRepository.findByReservationId(reservationId);

        // 성공한 결제가 있는 경우 환불 처리
        List<Payment> succeededPayments = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCEEDED)
                .toList();

        if (!succeededPayments.isEmpty()) {
            log.info("환불 처리 시작 - reservationId: {}, 결제 건수: {}",
                    reservationId, succeededPayments.size());

            for (Payment payment : succeededPayments) {
                processRefund(payment);
            }
        }
    }

    /**
     * 개별 결제 환불 처리
     */
    private void processRefund(Payment payment) {
        try {
            // 가상 환불 처리 (실제로는 외부 결제 시스템 호출)
            log.info("환불 처리 - paymentId: {}, amount: {}",
                    payment.getId(), payment.getAmount());

            log.info("환불 완료 - paymentId: {}", payment.getId());

        } catch (Exception e) {
            log.error("환불 처리 실패 - paymentId: {}, error: {}",
                    payment.getId(), e.getMessage(), e);

            throw new RuntimeException("환불 처리에 실패했습니다. 고객센터에 문의해주세요.");
        }
    }
}
