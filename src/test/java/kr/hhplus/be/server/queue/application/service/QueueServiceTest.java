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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    private QueueManager queueManager;
    private QueueAuditLogRepository logRepo;
    private UserJpaRepository userRepo;
    private ConcertJpaRepository concertRepo;

    private QueueService sut;

    @Captor
    ArgumentCaptor<QueueAuditLogEntity> logCaptor;

    @BeforeEach
    void setUp() {
        queueManager = mock(QueueManager.class);
        logRepo = mock(QueueAuditLogRepository.class);
        userRepo = mock(UserJpaRepository.class);
        concertRepo = mock(ConcertJpaRepository.class);

        sut = new QueueService(queueManager, logRepo, userRepo, concertRepo);
    }

    @Test
    void issue_success_should_delegate_to_queueManager_and_write_audit_log() {
        // given
        String userUuid = "user-uuid";
        Long concertId = 123L;

        QueueEntry issued = new QueueEntry("token-1", userUuid, concertId, 5L, QueueStatus.ISSUED);
        when(queueManager.addToQueue(userUuid, concertId)).thenReturn(issued);

        UserEntity user = mock(UserEntity.class);
        when(userRepo.findByUserUuid(userUuid)).thenReturn(Optional.of(user));
        ConcertEntity concert = mock(ConcertEntity.class);
        when(concertRepo.findById(concertId)).thenReturn(Optional.of(concert));

        // when
        QueueEntry result = sut.issue(userUuid, concertId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("token-1");
        assertThat(result.status()).isEqualTo(QueueStatus.ISSUED);

        verify(queueManager).addToQueue(userUuid, concertId);

        verify(logRepo).save(logCaptor.capture());
        QueueAuditLogEntity saved = logCaptor.getValue();
        assertThat(saved).isNotNull();

        verifyNoMoreInteractions(queueManager, logRepo, userRepo, concertRepo);
    }

    @Test
    void validateTokenOrThrow_success() {
        // given
        String userUuid = "u-1";
        Long concertId = 7L;
        String token = "t-1";

        QueueEntry entry = new QueueEntry(token, userUuid, concertId, 0L, QueueStatus.ACTIVE);
        when(queueManager.findByToken(token)).thenReturn(Optional.of(entry));

        // when / then
        assertThatCode(() -> sut.validateTokenOrThrow(userUuid, token))
                .doesNotThrowAnyException();

        verify(queueManager).findByToken(token);
        verifyNoMoreInteractions(queueManager, logRepo, userRepo, concertRepo);
    }

    @Test
    void validateTokenOrThrow_should_throw_when_user_mismatch() {
        // given
        String userUuid = "u-1";
        String token = "t-1";

        QueueEntry entry = new QueueEntry(token, "other-user", 1L, 0L, QueueStatus.ACTIVE);
        when(queueManager.findByToken(token)).thenReturn(Optional.of(entry));

        // expect
        assertThatThrownBy(() -> sut.validateTokenOrThrow(userUuid, token))
                .isInstanceOf(InvalidQueueTokenException.class)
                .hasMessageContaining("일치하지");

        verify(queueManager).findByToken(token);
        verifyNoMoreInteractions(queueManager, logRepo, userRepo, concertRepo);
    }

    @Test
    void validateTokenOrThrow_should_throw_when_expired() {
        // given
        String userUuid = "u-1";
        String token = "t-1";

        QueueEntry entry = new QueueEntry(token, userUuid, 1L, 0L, QueueStatus.EXPIRED);
        when(queueManager.findByToken(token)).thenReturn(Optional.of(entry));

        // expect
        assertThatThrownBy(() -> sut.validateTokenOrThrow(userUuid, token))
                .isInstanceOf(InvalidQueueTokenException.class)
                .hasMessageContaining("만료");

        verify(queueManager).findByToken(token);
        verifyNoMoreInteractions(queueManager, logRepo, userRepo, concertRepo);
    }

    @Test
    void expireUserForConcert_should_remove_and_write_audit_log() {
        // given
        String userUuid = "u-99";
        Long concertId = 99L;

        UserEntity user = mock(UserEntity.class);
        when(userRepo.findByUserUuid(userUuid)).thenReturn(Optional.of(user));
        ConcertEntity concert = mock(ConcertEntity.class);
        when(concertRepo.findById(concertId)).thenReturn(Optional.of(concert));

        // when
        sut.expireUserForConcert(userUuid, concertId);

        // then
        verify(queueManager).removeByUserAndConcert(userUuid, concertId);

        verify(logRepo).save(any(QueueAuditLogEntity.class));
        verifyNoMoreInteractions(queueManager, logRepo, userRepo, concertRepo);
    }

    @Test
    void findByToken_rankOf_statusOf_should_delegate_to_queueManager() {
        // given
        String token = "t-77";
        String userUuid = "u-77";

        when(queueManager.findByToken(token)).thenReturn(Optional.empty());
        when(queueManager.rankOf(token)).thenReturn(Optional.of(3L));
        when(queueManager.statusOf(token, userUuid)).thenReturn(QueueStatus.ISSUED);

        // when
        var found = sut.findByToken(token);
        var rank = sut.rankOf(token);
        var status = sut.statusOf(token, userUuid);

        // then
        assertThat(found).isEmpty();
        assertThat(rank).contains(3L);
        assertThat(status).isEqualTo(QueueStatus.ISSUED);

        verify(queueManager).findByToken(token);
        verify(queueManager).rankOf(token);
        verify(queueManager).statusOf(token, userUuid);
        verifyNoMoreInteractions(queueManager, logRepo, userRepo, concertRepo);
    }
}