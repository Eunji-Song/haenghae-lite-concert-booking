package kr.hhplus.be.server.queue.application.service;

import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.common.exception.queue.InvalidQueueTokenException;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertJpaRepository;
import kr.hhplus.be.server.queue.domain.model.QueueEntry;
import kr.hhplus.be.server.queue.infrastructure.jpa.entity.QueueAuditLogEntity;
import kr.hhplus.be.server.queue.infrastructure.jpa.repository.QueueAuditLogRepository;
import kr.hhplus.be.server.queue.infrastructure.memory.InMemoryQueueManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    private InMemoryQueueManager inMemory;
    private QueueAuditLogRepository logRepo;
    private UserJpaRepository userRepo;
    private ConcertJpaRepository concertRepo;

    private QueueService sut;

    @Captor
    ArgumentCaptor<QueueAuditLogEntity> logCaptor;

    @BeforeEach
    void setUp() {
        inMemory = mock(InMemoryQueueManager.class);
        logRepo = mock(QueueAuditLogRepository.class);
        userRepo = mock(UserJpaRepository.class);
        concertRepo = mock(ConcertJpaRepository.class);

        sut = new QueueService(inMemory, logRepo, userRepo, concertRepo);
    }

    @Test
    void issue_success_should_delegate_to_inmemory_and_write_audit_log() {
        // given
        String userUuid = "user-uuid";
        Long concertId = 123L;

        QueueEntry issued = new QueueEntry("token-1", userUuid, concertId, 5L, QueueStatus.ISSUED);
        when(inMemory.addToQueue(userUuid, concertId)).thenReturn(issued);

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

        verify(inMemory).addToQueue(userUuid, concertId);

        verify(logRepo).save(logCaptor.capture());
        QueueAuditLogEntity saved = logCaptor.getValue();
        assertThat(saved).isNotNull();
        // 필드 검증은 엔티티 게터 유무에 따라 조정
        // 여기서는 상태만 간단히 검증 가능하다고 가정
        // assertThat(saved.getStatus()).isEqualTo(QueueStatus.ISSUED);

        verifyNoMoreInteractions(inMemory, logRepo, userRepo, concertRepo);
    }

    @Test
    void validateTokenOrThrow_success() {
        // given
        String userUuid = "u-1";
        Long concertId = 7L;
        String token = "t-1";

        QueueEntry entry = new QueueEntry(token, userUuid, concertId, 0L, QueueStatus.ACTIVE);
        when(inMemory.findByToken(token)).thenReturn(Optional.of(entry));

        // when / then
        assertThatCode(() -> sut.validateTokenOrThrow(userUuid, token))
                .doesNotThrowAnyException();

        verify(inMemory).findByToken(token);
        verifyNoMoreInteractions(inMemory, logRepo, userRepo, concertRepo);
    }

    @Test
    void validateTokenOrThrow_should_throw_when_user_mismatch() {
        // given
        String userUuid = "u-1";
        String token = "t-1";

        QueueEntry entry = new QueueEntry(token, "other-user", 1L, 0L, QueueStatus.ACTIVE);
        when(inMemory.findByToken(token)).thenReturn(Optional.of(entry));

        // expect
        assertThatThrownBy(() -> sut.validateTokenOrThrow(userUuid, token))
                .isInstanceOf(InvalidQueueTokenException.class)
                .hasMessageContaining("일치하지");

        verify(inMemory).findByToken(token);
        verifyNoMoreInteractions(inMemory, logRepo, userRepo, concertRepo);
    }

    @Test
    void validateTokenOrThrow_should_throw_when_expired() {
        // given
        String userUuid = "u-1";
        String token = "t-1";

        QueueEntry entry = new QueueEntry(token, userUuid, 1L, 0L, QueueStatus.EXPIRED);
        when(inMemory.findByToken(token)).thenReturn(Optional.of(entry));

        // expect
        assertThatThrownBy(() -> sut.validateTokenOrThrow(userUuid, token))
                .isInstanceOf(InvalidQueueTokenException.class)
                .hasMessageContaining("만료");

        verify(inMemory).findByToken(token);
        verifyNoMoreInteractions(inMemory, logRepo, userRepo, concertRepo);
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
        verify(inMemory).removeByUserAndConcert(userUuid, concertId);

        verify(logRepo).save(any(QueueAuditLogEntity.class));
        verifyNoMoreInteractions(inMemory, logRepo, userRepo, concertRepo);
    }

    @Test
    void findByToken_rankOf_statusOf_should_delegate_to_inmemory() {
        // given
        String token = "t-77";
        String userUuid = "u-77";

        when(inMemory.findByToken(token)).thenReturn(Optional.empty());
        when(inMemory.rankOf(token)).thenReturn(Optional.of(3L));
        when(inMemory.statusOf(token, userUuid)).thenReturn(QueueStatus.ISSUED);

        // when
        var found = sut.findByToken(token);
        var rank = sut.rankOf(token);
        var status = sut.statusOf(token, userUuid);

        // then
        assertThat(found).isEmpty();
        assertThat(rank).contains(3L);
        assertThat(status).isEqualTo(QueueStatus.ISSUED);

        verify(inMemory).findByToken(token);
        verify(inMemory).rankOf(token);
        verify(inMemory).statusOf(token, userUuid);
        verifyNoMoreInteractions(inMemory, logRepo, userRepo, concertRepo);
    }
}