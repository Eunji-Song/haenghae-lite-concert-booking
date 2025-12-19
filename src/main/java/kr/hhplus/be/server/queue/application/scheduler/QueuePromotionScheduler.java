package kr.hhplus.be.server.queue.application.scheduler;

import kr.hhplus.be.server.queue.domain.repository.QueueManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;


import java.util.Set;

@Component
@RequiredArgsConstructor
@Profile("!test")
@ConditionalOnProperty(
        prefix = "queue.scheduler",
        name = "promotion-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class QueuePromotionScheduler {

    private final StringRedisTemplate redisTemplate;
    private final QueueManager queueManager;

    // issue 때 기록해둔 콘서트 목록
    private static final String KEY_CONCERTS = "queue:concerts";

    @Scheduled(fixedDelay = 5000) 
    public void promote() {
        Set<String> concertIds = redisTemplate.opsForSet().members(KEY_CONCERTS);
        if (concertIds == null || concertIds.isEmpty()) return;

        for (String cid : concertIds) {
            Long concertId = Long.valueOf(cid);
            queueManager.promote(concertId, 50);
        }
    }
}
