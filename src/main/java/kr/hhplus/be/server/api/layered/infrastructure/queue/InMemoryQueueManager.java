package kr.hhplus.be.server.api.layered.infrastructure.queue;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryQueueManager {

    // 콘서트별 대기열 관리 (concertId -> 대기 사용자 순서)
    private final Map<Long, Queue<QueueEntry>> waitingQueues = new ConcurrentHashMap<>();

    // 콘서트별 활성 사용자 관리 (concertId -> 활성 사용자 Set)
    private final Map<Long, Set<Long>> activeUsers = new ConcurrentHashMap<>();

    // 사용자별 대기열 정보 (userId_concertId -> QueueEntry)
    private final Map<String, QueueEntry> userQueueInfo = new ConcurrentHashMap<>();

    // 대기열 번호 생성기
    private final AtomicLong queueNumberGenerator = new AtomicLong(0);


    /**
     * 사용자를 대기열에 추가
     */
    public QueueEntry addToQueue(Long userId, Long concertId) {
        String userKey = generateUserKey(userId, concertId);

        // 이미 대기중인지 확인
        if (userQueueInfo.containsKey(userKey)) {
            throw new IllegalStateException("이미 대기열에 등록된 사용자입니다.");
        }

        QueueEntry entry = new QueueEntry(
                userId,
                concertId,
                queueNumberGenerator.incrementAndGet(),
                LocalDateTime.now()
        );

        // 대기열에 추가
        waitingQueues.computeIfAbsent(concertId, k -> new ConcurrentLinkedQueue<>()).offer(entry);
        userQueueInfo.put(userKey, entry);

        return entry;
    }


    /**
     * 사용자의 대기 순번 조회
     */
    public Long getQueueRank(Long userId, Long concertId) {
        Queue<QueueEntry> queue = waitingQueues.get(concertId);
        if (queue == null) return null;

        long rank = 0;
        for (QueueEntry entry : queue) {
            if (entry.getUserId().equals(userId)) {
                return rank;
            }
            rank++;
        }

        return null; // 대기열에 없음
    }


    /**
     * 대기열 크기 조회
     */
    public int getQueueSize(Long concertId) {
        Queue<QueueEntry> queue = waitingQueues.get(concertId);
        return queue != null ? queue.size() : 0;
    }

    /**
     * 사용자를 활성 상태로 변경
     */
    public boolean activateUser(Long userId, Long concertId) {
        String userKey = generateUserKey(userId, concertId);
        QueueEntry entry = userQueueInfo.get(userKey);

        if (entry == null) {
            return false; // 대기열에 없음
        }

        // 대기열에서 제거
        Queue<QueueEntry> queue = waitingQueues.get(concertId);
        if (queue != null) {
            queue.removeIf(e -> e.getUserId().equals(userId));
        }

        // 활성 사용자로 추가
        activeUsers.computeIfAbsent(concertId, k -> ConcurrentHashMap.newKeySet()).add(userId);

        // 사용자 정보에서 제거
        userQueueInfo.remove(userKey);

        return true;
    }

    /**
     * 사용자가 활성 상태인지 확인
     */
    public boolean isActiveUser(Long userId, Long concertId) {
        Set<Long> active = activeUsers.get(concertId);
        return active != null && active.contains(userId);
    }

    /**
     * 사용자가 대기중인지 확인
     */
    public boolean isInQueue(Long userId, Long concertId) {
        String userKey = generateUserKey(userId, concertId);
        return userQueueInfo.containsKey(userKey);
    }

    /**
     * 활성 사용자 수 조회
     */
    public int getActiveUserCount(Long concertId) {
        Set<Long> active = activeUsers.get(concertId);
        return active != null ? active.size() : 0;
    }


    /**
     * 배치 기능
     * 다음 활성화할 사용자들 처리
     */
    public List<QueueEntry> activateNextUsers(Long concertId, int count) {
        Queue<QueueEntry> queue = waitingQueues.get(concertId);
        if (queue == null || queue.isEmpty()) {
            return Collections.emptyList();
        }

        List<QueueEntry> activated = new ArrayList<>();

        for (int i = 0; i < count && !queue.isEmpty(); i++) {
            QueueEntry entry = queue.poll();
            if (entry != null) {
                activateUser(entry.getUserId(), concertId);
                activated.add(entry);
            }
        }

        return activated;
    }

    /**
     * 사용자를 대기열/활성 상태에서 제거 (로그아웃, 토큰 만료 등)
     */
    public void removeUser(Long userId, Long concertId) {
        String userKey = generateUserKey(userId, concertId);

        // 대기열에서 제거
        Queue<QueueEntry> queue = waitingQueues.get(concertId);
        if (queue != null) {
            queue.removeIf(e -> e.getUserId().equals(userId));
        }

        // 활성 사용자에서 제거
        Set<Long> active = activeUsers.get(concertId);
        if (active != null) {
            active.remove(userId);
        }

        // 사용자 정보 제거
        userQueueInfo.remove(userKey);
    }


    // 공통

    private String generateUserKey(Long userId, Long concertId) {
        return userId + "_" + concertId;
    }





}