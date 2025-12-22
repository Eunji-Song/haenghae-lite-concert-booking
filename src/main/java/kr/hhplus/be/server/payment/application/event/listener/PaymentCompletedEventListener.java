package kr.hhplus.be.server.payment.application.event.listener;

import kr.hhplus.be.server.payment.application.event.PaymentCompletedEvent;
import kr.hhplus.be.server.payment.application.port.out.PaymentEventOutPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * 결제 완료 이벤트 리스너
 *
 * - 결제 트랜잭션이 "성공적으로 커밋된 이후"에만 실행된다.
 * - 결제 도메인의 핵심 트랜잭션(결제/예약확정/토큰만료)과
 *   외부 전송(mock API 호출) 같은 부가 관심사를 분리한다.
 */
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCompletedEventListener.class);

    private final PaymentEventOutPort paymentEventOutPort;

    /**
     * 결제 완료 이벤트를 받아 외부 데이터 플랫폼(mock API)으로 결제/예약 정보를 전송한다.
     *
     * - AFTER_COMMIT: 커밋이 완료된 뒤 실행되어야 외부 시스템에 "확정된 결제"만 전송할 수 있다.
     * - 외부 전송 실패가 결제 트랜잭션 롤백에 영향을 주지 않도록 분리한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        try {
            paymentEventOutPort.sendPaymentCompleted(event);
        } catch (Exception e) {
            // 과제 범위에서는 로깅 정도로 처리하고,
            // 실무에서는 재시도/적재(Outbox)/DLQ 등으로 확장 가능하다.
            log.warn("Failed to send payment completed event. paymentId={}, reservationId={}, idemKey={}",
                    event.paymentId(), event.reservationId(), event.idempotencyKey(), e);
        }
    }
}