package kr.hhplus.be.server.payment.infrastructure.persistence.jpa.adapter;

import kr.hhplus.be.server.payment.application.port.out.QueuePort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Profile("prod")
@Component
@RequiredArgsConstructor
public class QueueRestAdapter implements QueuePort {

    private final RestClient restClient = RestClient.create(); // 필요 시 Bean 주입/설정

    @Override
    public void validate(String userUuid, String queueToken) {
        // 예: 대기열 서버의 검증 API 호출 (엔드포인트/스펙은 너의 실제 프로젝트에 맞추기)
        restClient.get()
                .uri("http://queue-service/internal/queue/validate?userUuid={userUuid}", userUuid)
                .header("X-Queue-Token", queueToken)
                .retrieve()
                .toBodilessEntity();
        // 유효하지 않으면 4xx/5xx -> RestClient가 예외 발생 → 상위 서비스에서 처리
    }

    @Override
    public void expireToken(String userUuid, Long concertId) {
        restClient.post()
                .uri("http://queue-service/internal/queue/expire?userUuid={u}&concertId={c}", userUuid, concertId)
                .retrieve()
                .toBodilessEntity();
    }
}