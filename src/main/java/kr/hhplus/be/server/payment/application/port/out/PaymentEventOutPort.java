package kr.hhplus.be.server.payment.application.port.out;

import kr.hhplus.be.server.payment.application.event.PaymentCompletedEvent;

/**
 * 결제 완료 이벤트 외부 전송 포트
 *
 * - 외부 데이터 플랫폼(mock API) 호출을 추상화한다.
 * - application 레이어는 인프라 구현(HTTP/Feign/WebClient 등)에 직접 의존하지 않는다.
 */
public interface PaymentEventOutPort {
    void sendPaymentCompleted(PaymentCompletedEvent event);
}