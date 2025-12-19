package kr.hhplus.be.server.queue.application.service;

import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.common.exception.queue.InvalidQueueTokenException;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertJpaRepository;
import kr.hhplus.be.server.queue.domain.model.QueueEntry;
import kr.hhplus.be.server.queue.domain.repository.QueueManager;
import kr.hhplus.be.server.queue.infrastructure.jpa.entity.QueueAuditLogEntity;
import kr.hhplus.be.server.queue.infrastructure.jpa.repository.QueueAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 대기열 유스케이스 서비스.
 * - QueueManager(인메모리/Redis 등)와 JPA 감사로그(QueueAuditLogRepository)를 함께 다룬다.
 * - 발급/검증/상태조회/만료를 단일 파사드로 제공한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueService {

    private final QueueManager queueManager;
    private final QueueAuditLogRepository logRepo;
    private final UserJpaRepository userRepo;
    private final ConcertJpaRepository concertRepo;

    /** 토큰 발급 (대기열 진입) */
    @Transactional
    public QueueEntry issue(String userUuid, Long concertId) {
        QueueEntry entry = queueManager.addToQueue(userUuid, concertId);

        // 감사 로그 적재
        var user = requiredUser(userUuid);
        var concert = requiredConcert(concertId);
        logRepo.save(QueueAuditLogEntity.of(
                user, concert, entry.status(), (int) entry.rank(), "ISSUE_TOKEN"));

        return entry;
    }

    /** 토큰으로 QueueEntry 조회 */
    public Optional<QueueEntry> findByToken(String token) {
        return queueManager.findByToken(token);
    }

    /** 토큰 순위 조회 (0이 맨 앞) */
    public Optional<Long> rankOf(String token) {
        return queueManager.rankOf(token);
    }

    /** 토큰 상태 조회 */
    public QueueStatus statusOf(String token, String userUuid) {
        return queueManager.statusOf(token, userUuid);
    }

    /** 결제 완료 등으로 사용자-콘서트 조합의 토큰 만료 처리 */
    @Transactional
    public void expireUserForConcert(String userUuid, Long concertId) {
        queueManager.removeByUserAndConcert(userUuid, concertId);

        // 감사 로그 적재
        var user = requiredUser(userUuid);
        var concert = requiredConcert(concertId);
        logRepo.save(QueueAuditLogEntity.of(
                user, concert, QueueStatus.EXPIRED, null, "EXPIRE_BY_PAYMENT"));
    }

    /**
     * 결제/예약 측에서 사용할 검증(무효면 예외).
     * - 토큰이 존재하지 않거나 사용자 불일치, 만료인 경우 InvalidQueueTokenException 발생.
     */
    public void validateTokenOrThrow(String userUuid, String queueToken) {
        var entry = queueManager.findByToken(queueToken)
                .orElseThrow(InvalidQueueTokenException::new);

        if (!entry.userUuid().equals(userUuid)) {
            throw new InvalidQueueTokenException("사용자와 토큰이 일치하지 않습니다.");
        }
        if (entry.status() == QueueStatus.EXPIRED) {
            throw new InvalidQueueTokenException("만료된 토큰입니다.");
        }
    }

    /**
     * 간단 검증 헬퍼: 만료(EXPIRED)이면 예외.
     * (ACTIVE/ISSUED 모두 통과시키고 싶을 때 사용)
     */
    public void validateActive(String userUuid, String queueToken) {
        QueueStatus status = statusOf(queueToken, userUuid);
        if (status == QueueStatus.EXPIRED) {
            throw new InvalidQueueTokenException();
        }
    }

    private UserEntity requiredUser(String userUuid) {
        return userRepo.findByUserUuid(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("USER not found: " + userUuid));
    }

    private ConcertEntity requiredConcert(Long concertId) {
        return concertRepo.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("CONCERT not found: " + concertId));
    }
}