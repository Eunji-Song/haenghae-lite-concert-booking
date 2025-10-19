package kr.hhplus.be.server.api.clean.application.usecase;

import kr.hhplus.be.server.api.clean.application.port.in.payment.PayForReservationCommand;
import kr.hhplus.be.server.api.clean.application.port.in.payment.PaymentResponse;
import kr.hhplus.be.server.api.clean.application.port.out.payment.PaymentRepository;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.clean.domain.model.payment.Payment;
import kr.hhplus.be.server.api.clean.domain.model.reservation.Reservation;
import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.reservation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayForReservationUseCase {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    /**
     * 예약에 대한 결제 및 확정 처리
     *
     * @param command 결제 요청 정보
     * @return 결제 결과
     * @throws ReservationNotFoundException 예약을 찾을 수 없는 경우
     * @throws ReservationHoldExpiredException 홀드 시간이 만료된 경우
     * @throws PaymentAmountMismatchException 결제 금액이 일치하지 않는 경우
     */
    @Transactional
    public PaymentResponse payForReservation(PayForReservationCommand command) {
        log.info("결제 요청 시작 - reservationId: {}, amount: {}",
                command.reservationId(), command.amount());

        // 예약 존재 여부 및 조회
        Reservation reservation = findReservationOrThrow(command.reservationId());

        // 예약 상태 및 만료 시간 검증
        validateReservationForPayment(reservation);

        // 결제 금액 검증
        validatePaymentAmount(reservation, command.amount());

        // 결제 처리
        Payment payment = processPayment(command);

        // 예약 확정 처리
        reservation.confirm();
        Reservation confirmedReservation = reservationRepository.save(reservation);

        log.info("결제 및 예약 확정 완료 - paymentId: {}, reservationId: {}",
                payment.getId(), confirmedReservation.getId());

        return new PaymentResponse(
                payment.getId(),
                confirmedReservation.getId(),
                true
        );
    }

    /**
     * 예약 조회 및 존재 여부 확인
     */
    private Reservation findReservationOrThrow(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }

    /**
     * 결제 가능한 예약 상태인지 검증
     */
    private void validateReservationForPayment(Reservation reservation) {
        if (!reservation.isPayable()) {
            if (reservation.isExpired()) {
                throw new ReservationHoldExpiredException();
            }

            // PENDING이 아닌 상태인 경우 - 도메인에서 발생할 예외를 미리 방지하거나 명시적 예외 발생
            throw new InvalidReservationStatusException("결제 가능한 상태가 아닙니다");
        }
    }

    /**
     * 결제 금액과 예약 금액 일치 여부 확인
     */
    private void validatePaymentAmount(Reservation reservation, Long paymentAmount) {
        if (!reservation.getAmount().equals(paymentAmount)) {
            throw new PaymentAmountMismatchException(reservation.getAmount(), paymentAmount);
        }
    }

    /**
     * 결제 처리 (가상 결제)
     */
    private Payment processPayment(PayForReservationCommand command) {
        // 멱등성 키 생성 (실제로는 요청 헤더에서 받아오는 것이 좋음)
        String idempotencyKey = generateIdempotencyKey(command.reservationId());

        // 중복 결제 확인
        paymentRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(existingPayment -> {
                    throw new IllegalStateException("이미 처리된 결제 요청입니다: " + idempotencyKey);
                });

        // 결제 도메인 객체 생성
        Payment payment = Payment.createPendingPayment(
                command.reservationId(),
                command.amount(),
                idempotencyKey
        );

        // 가상 결제 처리 (항상 성공)
        payment.succeed();

        // 결제 저장
        return paymentRepository.save(payment);
    }

    /**
     * 멱등성 키 생성
     * 실제로는 HTTP 헤더 Idempotency-Key에서 받아와야 함
     */
    private String generateIdempotencyKey(Long reservationId) {
        return String.format("pay_%d_%s", reservationId, UUID.randomUUID().toString().substring(0, 8));
    }
}