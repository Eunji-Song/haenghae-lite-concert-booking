package kr.hhplus.be.server.queue.infrastructure.memory;

import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.queue.domain.model.QueueEntry;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 간단한 인메모리 큐:
 * - 콘서트별 카운터로 순번 부여
 * - 토큰 → 엔트리 맵
 * - 정책 단순화를 위해 ACTIVE 판정은 rank == 0 으로 가정(필요시 윈도우 사이즈 적용)
 */
@Component
public class InMemoryQueueManager {

    private final Map<Long, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, QueueEntry> tokenMap = new ConcurrentHashMap<>();

    public QueueEntry addToQueue(String userUuid, Long concertId) {
        long rank = counters.computeIfAbsent(concertId, k -> new AtomicLong(0)).getAndIncrement();
        String token = UUID.randomUUID().toString();
        QueueEntry e = new QueueEntry(token, userUuid, concertId, rank,
                (rank == 0 ? QueueStatus.ACTIVE : QueueStatus.ISSUED));
        tokenMap.put(token, e);
        return e;
    }

    public Optional<QueueEntry> findByToken(String token) {
        return Optional.ofNullable(tokenMap.get(token));
    }

    public Optional<Long> rankOf(String token) {
        return findByToken(token).map(QueueEntry::rank);
    }

    public QueueStatus statusOf(String token, String userUuid) {
        QueueEntry e = tokenMap.get(token);
        if (e == null) return QueueStatus.EXPIRED;
        if (!Objects.equals(e.userUuid(), userUuid)) return QueueStatus.EXPIRED;
        return e.status();
    }

    /** 결제 완료 등으로 토큰 만료 */
    public void removeByUserAndConcert(String userUuid, Long concertId) {
        tokenMap.values().removeIf(e -> Objects.equals(e.userUuid(), userUuid)
                && Objects.equals(e.concertId(), concertId));
    }
}