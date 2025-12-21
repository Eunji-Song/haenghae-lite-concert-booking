package kr.hhplus.be.server.payment.infrastructure.dataplatform;

import kr.hhplus.be.server.payment.application.event.PaymentCompletedEvent;
import kr.hhplus.be.server.payment.application.port.out.PaymentEventOutPort;
import kr.hhplus.be.server.payment.infrastructure.dataplatform.dto.PaymentCompletedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Profile("test")
@RequiredArgsConstructor
public class PaymentEventOutAdapter implements PaymentEventOutPort {

    private final RestTemplate restTemplate;

    @Value("${external.data-platform.base-url}")
    private String baseUrl;

    @Override
    public void sendPaymentCompleted(PaymentCompletedEvent event) {
        String url = baseUrl + "/mock/payments/completed";

        PaymentCompletedPayload payload = new PaymentCompletedPayload(
                event.paymentId(),
                event.reservationId(),
                event.userUuid(),
                event.concertId(),
                event.amount(),
                event.idempotencyKey(),
                event.occurredAt()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PaymentCompletedPayload> request = new HttpEntity<>(payload, headers);

        // mock API는 보통 200 OK 또는 201 Created만 받으면 성공으로 본다
        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Data platform response not successful. status=" + response.getStatusCode());
        }
    }
}