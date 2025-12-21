package kr.hhplus.be.server.payment.application.usecase;

import kr.hhplus.be.server.payment.application.event.PaymentCompletedEvent;
import kr.hhplus.be.server.payment.application.port.in.command.PayForReservationCommand;
import kr.hhplus.be.server.payment.application.port.in.result.PaymentResponse;
import kr.hhplus.be.server.payment.application.port.in.usecase.PayForReservationUseCase;
import kr.hhplus.be.server.payment.application.port.out.*;
import kr.hhplus.be.server.payment.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * 결제 + 예약 확정 유스케이스 구현체
 *
 * - 결제 트랜잭션의 핵심 로직만 담당
 * - 결제 완료 이후의 부가 작업(외부 전송 등)은 이벤트로 분리
 */
@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PayForReservationService implements PayForReservationUseCase {

    // ===== 외부 도메인 협력 포트 =====
    private final QueuePort queuePort;               // 대기열 검증/만료
    private final ReservationPort reservationPort;   // 예약 조회/확정
    private final WalletPort walletPort;             // 지갑 차감
    private final PaymentRepository paymentRepository; // 결제 저장

    // ===== 이벤트 & 시간 의존성 =====
    private final ApplicationEventPublisher eventPublisher; // 결제 완료 이벤트 발행
    private final Clock clock;                               // 시간 제어(테스트/운영 일관성)

    /**
     * 결제 수행 메서드
     *
     * 흐름:
     * 1. 대기열 토큰 검증
     * 2. 예약 조회 및 결제 가능 상태 확인
     * 3. 멱등성 체크
     * 4. 지갑 차감
     * 5. 결제 저장
     * 6. 예약 확정
     * 7. 대기열 토큰 만료
     * 8. 결제 완료 이벤트 발행 (트랜잭션 종료 후 후속 처리용)
     */
    @Override
    public PaymentResponse pay(PayForReservationCommand cmd) {

        // 1. 대기열 토큰 유효성 검증
        queuePort.validate(cmd.userUuid(), cmd.queueToken());

        // 2. 결제 대상 예약 조회 및 검증
        ReservationForPayment res = reservationPort.getReservationForPayment(
                cmd.reservationId(), cmd.userUuid()
        );

        // 3. 멱등 키 생성/확인 (중복 결제 방지)
        String idem = (cmd.idempotencyKey() != null && !cmd.idempotencyKey().isBlank())
                ? cmd.idempotencyKey()
                : UUID.randomUUID().toString();

        if (paymentRepository.existsByIdempotencyKey(idem)) {
            // 이미 처리된 결제 요청이면
            // - 결제 로직 재실행 하지 않음
            // - 이벤트 재발행 하지 않음
            return new PaymentResponse(null, res.reservationId(), true);
        }

        // 4. 지갑 차감 (실패 시 예외 발생 → 트랜잭션 롤백)
        walletPort.debit(res.userId(), res.amount(), null, idem);

        // 5. 결제 성공 정보 저장
        Payment saved = paymentRepository.save(
                Payment.succeeded(res.reservationId(), res.userId(), res.amount(), idem)
        );

        // 6. 예약 상태를 확정 상태로 변경
        reservationPort.confirm(res.reservationId(), cmd.userUuid());

        // 7. 결제 완료 후 대기열 토큰 만료 처리
        queuePort.expireToken(cmd.userUuid(), res.concertId());

        // 8. 결제 완료 이벤트 발행 - 트랜잭션 커밋 이후 외부 시스템 연동을 위한 신호
        eventPublisher.publishEvent(new PaymentCompletedEvent(
                saved.getId(),
                res.reservationId(),
                cmd.userUuid(),
                res.concertId(),
                res.amount(),
                idem,
                clock.instant()
        ));

        // 9. 결제 응답 반환
        return new PaymentResponse(saved.getId(), res.reservationId(), true);
    }
}