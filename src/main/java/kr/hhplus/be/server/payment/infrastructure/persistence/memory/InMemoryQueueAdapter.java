package kr.hhplus.be.server.payment.infrastructure.persistence.memory;

import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.common.exception.queue.InvalidQueueTokenException;
import kr.hhplus.be.server.payment.application.port.out.QueuePort;
import kr.hhplus.be.server.queue.application.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * QueuePort의 인메모리 구현체 어댑터.
 * - QueueService(InMemoryQueueManager 기반)를 호출한다.
 * - 이후 Redis로 교체 시 이 어댑터만 교체하면 된다.
 */
@Component
@RequiredArgsConstructor
public class InMemoryQueueAdapter implements QueuePort {

    private final QueueService queueService;

    @Override
    public void validate(String userUuid, String queueToken) {
        var entry = queueService.findByToken(queueToken)
                .orElseThrow(InvalidQueueTokenException::new);

        if (!entry.userUuid().equals(userUuid)) {
            throw new InvalidQueueTokenException("사용자와 대기열 토큰이 일치하지 않습니다.");
        }

        if (entry.status() == QueueStatus.EXPIRED) {
            throw new InvalidQueueTokenException("만료된 대기열 토큰입니다.");
        }
        // 필요 시 ACTIVE만 허용하도록 정책 강화 가능
    }

    @Override
    public void expireToken(String userUuid, Long concertId) {
        queueService.expireUserForConcert(userUuid, concertId);
    }
}