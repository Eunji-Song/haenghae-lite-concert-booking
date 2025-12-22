package kr.hhplus.be.server.payment.infrastructure.http;

import kr.hhplus.be.server.payment.application.event.PaymentCompletedEvent;
import kr.hhplus.be.server.payment.application.port.out.PaymentEventOutPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class HttpPaymentEventOutAdapter implements PaymentEventOutPort {

    private final RestTemplate restTemplate;

    @Value("${external.data-platform.base-url}")
    private String baseUrl;

    @Override
    public void sendPaymentCompleted(PaymentCompletedEvent event) {
        // 같은 프로젝트 안에 mock endpoint를 둔다면 base-url을 http://localhost:8080 로 두고
        // 아래 path로 호출하면 됨.
        String url = baseUrl + "/mock/data-platform/payments/completed";
        restTemplate.postForEntity(url, event, Void.class);
    }
}