package kr.hhplus.be.server.payment.infrastructure.persistence.jpa.adapter;

import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.common.exception.queue.InvalidQueueTokenException;
import kr.hhplus.be.server.payment.application.port.out.QueuePort;
import kr.hhplus.be.server.queue.application.service.QueueService;
import kr.hhplus.be.server.queue.domain.model.QueueEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * QueuePort 구현체.
 * 현재는 InMemoryQueueManager 기반 QueueService를 사용하여 토큰 검증/만료를 처리한다.
 * 향후 Redis 기반으로 교체 가능.
 */
@Profile("redis")
@Component
@RequiredArgsConstructor
public class QueueAdapter implements QueuePort {

    private final QueueService queueService;

    /**
     * 토큰이 존재하고, 해당 userUuid와 concertId가 일치하며
     * 상태가 ACTIVE 또는 ISSUED인 경우만 유효.
     */
    @Override
    public void validate(String userUuid, String queueToken) {
        QueueEntry entry = queueService.findByToken(queueToken)
                .orElseThrow(() -> new InvalidQueueTokenException("유효하지 않은 대기열 토큰입니다."));

        if (!entry.userUuid().equals(userUuid)) {
            throw new InvalidQueueTokenException("사용자와 대기열 토큰이 일치하지 않습니다.");
        }

        if (entry.status() == QueueStatus.EXPIRED) {
            throw new InvalidQueueTokenException("만료된 대기열 토큰입니다.");
        }
    }

    /**
     * 결제 완료 후 사용자의 대기열 토큰을 만료 처리.
     */
    @Override
    public void expireToken(String userUuid, Long concertId) {
        queueService.expireUserForConcert(userUuid, concertId);
    }
}