package kr.hhplus.be.server.api.layered.service.queue;


import kr.hhplus.be.server.api.layered.dto.queue.QueueTokenRequest;
import kr.hhplus.be.server.api.layered.dto.queue.QueueTokenResponse;
import kr.hhplus.be.server.api.layered.entity.concert.ConcertEntity;
import kr.hhplus.be.server.api.layered.entity.queue.QueueAuditLogEntity;
import kr.hhplus.be.server.api.layered.entity.user.UserEntity;
import kr.hhplus.be.server.api.layered.infrastructure.queue.InMemoryQueueManager;
import kr.hhplus.be.server.api.layered.infrastructure.queue.QueueEntry;
import kr.hhplus.be.server.api.layered.repository.queue.QueueAuditLogRepository;
import kr.hhplus.be.server.api.layered.service.concert.ConcertService;
import kr.hhplus.be.server.api.layered.service.user.UserService;
import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.common.exception.AlreadyInQueueException;
import kr.hhplus.be.server.common.exception.ConcertNotAvailableException;
import kr.hhplus.be.server.common.exception.InvalidQueueTokenException;
import kr.hhplus.be.server.common.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueService {

    private final InMemoryQueueManager inMemoryQueueManager;
    private final UserService userService;
    private final ConcertService concertService;
    private final JwtTokenProvider jwtTokenProvider;
    private final QueueAuditLogRepository queueAuditLogRepository;

    @Transactional
    public QueueTokenResponse enterQueue(String userUuid, QueueTokenRequest request) {
        // 사용자 검증
        UserEntity user = userService.getUser(userUuid);

        // 콘서트 유효성 검증
        ConcertEntity concert = concertService.getIsAvailableConcert(request.getConcertId());

        // 대기열 입장 여부 검증
        if (inMemoryQueueManager.isInQueue(user.getId(), request.getConcertId())) {
            throw new AlreadyInQueueException();
        }

        // 대기열 입장
        QueueEntry queueEntry = inMemoryQueueManager.addToQueue(user.getId(), request.getConcertId());

        // 대기 순번 조회
        Long rank = inMemoryQueueManager.getQueueRank(user.getId(), request.getConcertId());

        // 예상 대기 시간 계산
        Long etaSeconds = calculateEtaSeconds(rank);

        // 토큰 생성
        Map<String, Object> claims = Map.of(
                "rank", rank,
                "enteredAt", queueEntry.getEnteredAt().toString()
        );
        String queueToken = jwtTokenProvider.createQueueToken(userUuid, request.getConcertId(), claims);

        // 대기열 로그 저장
        saveQueueAuditLog(user, concert, QueueStatus.ISSUED, rank);

        return new QueueTokenResponse(
                queueToken,
                QueueStatus.ISSUED,
                rank,
                etaSeconds
        );
    }


    // 대기열 상태 조회
    public QueueTokenResponse getQueueStatus(String queueToken) {
        try {
            // 토큰 파싱
            var claims = jwtTokenProvider.parseQueue(queueToken);
            String userUuid = claims.getBody().getSubject();
            Long concertId = Long.valueOf(claims.getBody().get("concertId").toString());

            // 사용자 검증
            UserEntity user = userService.getUser(userUuid);

            // 활성 여부
            if (inMemoryQueueManager.isActiveUser(user.getId(), concertId)) {
                return new QueueTokenResponse(queueToken, QueueStatus.ACTIVE);
            }

            // 대기열에 있는지 확인
            if (!inMemoryQueueManager.isInQueue(user.getId(), concertId)) {
                // 만료된 경우
                return new QueueTokenResponse(queueToken, QueueStatus.EXPIRED);
            }

            // 현재 대기 순번 조회
            Long currentRank = inMemoryQueueManager.getQueueRank(user.getId(), concertId);
            Long etaSeconds = calculateEtaSeconds(currentRank);

            return new QueueTokenResponse(queueToken, QueueStatus.ISSUED, currentRank, etaSeconds);

        } catch (Exception e) {
            throw new InvalidQueueTokenException();
        }
    }

    /**
     * 배치용 - 대기열 순번을 활성화
     */
    @Transactional
    public void activateNextUsers(Long concertId, int count) {
        ConcertEntity concert = concertService.getIsAvailableConcert(concertId);

        var activatedEntries = inMemoryQueueManager.activateNextUsers(concertId, count);

        // 활성화된 사용자들의 로그 기록
        for (QueueEntry entry : activatedEntries) {
            UserEntity user = userService.getUser(entry.getUserId());

            saveQueueAuditLog(user, concert, QueueStatus.ACTIVE, null);
        }
    }

    /**
     * 사용자 대기열에서 제거 (토큰 만료, 예약 완료 등)
     */
    @Transactional
    public void removeUserFromQueue(String userUuid, ConcertEntity concert) {
        UserEntity user = userService.getUser(userUuid);
        inMemoryQueueManager.removeUser(user.getId(), concert.getId());

        saveQueueAuditLog(user, concert, QueueStatus.EXPIRED, null);
    }

    /**
     * ETA(예상 대기 시간) 계산
     */
    private Long calculateEtaSeconds(Long rank) {
        if (rank == null) return null;
        return rank * 30;
    }

    /**
     * 대기열 감사 로그 저장
     */
    private void saveQueueAuditLog(UserEntity user, ConcertEntity concert, QueueStatus status, Long rank) {
        QueueAuditLogEntity auditLog = QueueAuditLogEntity.builder()
                .user(user)
                .concert(concert)
                .status(status)
                .rankPosition(rank)
                .note("Queue status changed to " + status)
                .build();

        queueAuditLogRepository.save(auditLog);
    }
}