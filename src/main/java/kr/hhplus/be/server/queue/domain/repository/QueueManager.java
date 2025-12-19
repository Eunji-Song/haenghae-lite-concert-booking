package kr.hhplus.be.server.queue.domain.repository;

import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.queue.domain.model.QueueEntry;

import java.util.Optional;

/**
 * 대기열 저장소 인터페이스.
 * - 구현체 예: 인메모리(InMemoryQueueManager), Redis 기반 QueueManager
 */
public interface QueueManager {

    /** 대기열 진입 (토큰 발급 + 순번 부여) */
    QueueEntry addToQueue(String userUuid, Long concertId);

    /** 토큰으로 엔트리 조회 */
    Optional<QueueEntry> findByToken(String token);

    /** 토큰의 대기 순번 조회 (0이 맨 앞) */
    Optional<Long> rankOf(String token);

    /** 토큰 상태 조회 (ACTIVE / ISSUED / EXPIRED 등) */
    QueueStatus statusOf(String token, String userUuid);

    /** 결제 완료 등으로 사용자-콘서트 조합의 토큰 만료 처리 */
    void removeByUserAndConcert(String userUuid, Long concertId);

    /** 대기열 승급 (Waiting -> Active) */
    long promote(Long concertId, int maxToPromote);
}