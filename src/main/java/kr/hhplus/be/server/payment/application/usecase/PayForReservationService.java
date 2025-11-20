package kr.hhplus.be.server.payment.application.usecase;

import kr.hhplus.be.server.payment.application.port.in.command.PayForReservationCommand;
import kr.hhplus.be.server.payment.application.port.in.result.PaymentResponse;
import kr.hhplus.be.server.payment.application.port.in.usecase.PayForReservationUseCase;
import kr.hhplus.be.server.payment.application.port.out.*;
import kr.hhplus.be.server.payment.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PayForReservationService implements PayForReservationUseCase {

    private final QueuePort queuePort;
    private final ReservationPort reservationPort;
    private final WalletPort walletPort;
    private final PaymentRepository paymentRepository;

    @Override
    public PaymentResponse pay(PayForReservationCommand cmd) {
        // 대기열 검증
        queuePort.validate(cmd.userUuid(), cmd.queueToken());

        // 예약 로딩/검증
        ReservationForPayment res = reservationPort.getReservationForPayment(
                cmd.reservationId(), cmd.userUuid()
        );

        // 멱등키 확인
        String idem = (cmd.idempotencyKey() != null && !cmd.idempotencyKey().isBlank())
                ? cmd.idempotencyKey()
                : UUID.randomUUID().toString();
        if (paymentRepository.existsByIdempotencyKey(idem)) {
            return new PaymentResponse(null, res.reservationId(), true);
        }

        // 지갑 차감
        walletPort.debit(res.userId(), res.amount(), null, idem);

        // 결제 저장 (성공)
        Payment saved = paymentRepository.save(
                Payment.succeeded(res.reservationId(), res.userId(), res.amount(), idem)
        );

        // 예약 확정
        reservationPort.confirm(res.reservationId(), cmd.userUuid());

        // 대기열 토큰 만료
        queuePort.expireToken(cmd.userUuid(), res.concertId());

        return new PaymentResponse(saved.getId(), res.reservationId(), true);
    }
}