package kr.hhplus.be.server.api.layered.service.queue;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import kr.hhplus.be.server.api.layered.dto.queue.QueueTokenRequest;
import kr.hhplus.be.server.api.layered.dto.queue.QueueTokenResponse;
import kr.hhplus.be.server.common.entity.concert.ConcertEntity;
import kr.hhplus.be.server.api.layered.entity.queue.QueueAuditLogEntity;
import kr.hhplus.be.server.api.layered.entity.user.UserEntity;
import kr.hhplus.be.server.api.layered.infrastructure.queue.InMemoryQueueManager;
import kr.hhplus.be.server.api.layered.infrastructure.queue.QueueEntry;
import kr.hhplus.be.server.api.layered.repository.queue.QueueAuditLogRepository;
import kr.hhplus.be.server.api.layered.service.concert.ConcertService;
import kr.hhplus.be.server.api.layered.service.user.UserService;
import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.common.exception.queue.AlreadyInQueueException;
import kr.hhplus.be.server.common.exception.concert.ConcertNotAvailableException;
import kr.hhplus.be.server.common.exception.queue.InvalidQueueTokenException;
import kr.hhplus.be.server.common.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @InjectMocks
    QueueService queueService;

    @Mock
    private UserService userService;
    @Mock
    private ConcertService concertService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private QueueAuditLogRepository queueAuditLogRepository;
    @Mock
    private InMemoryQueueManager inMemoryQueueManager;

    private UserEntity user(long id, String uuid) {
        return UserEntity.builder()
                .id(id)
                .userUuid(uuid)
                .email("test@example.com")
                .password("{bcrypt}encoded")
                .name("테스터")
                .build();
    }

    private ConcertEntity concert(Long id) {
        return ConcertEntity.builder()
                .id(id)
                .title("테스트 콘서트")
                .description("상세 내용")
                .artistName("Artist")
                .organizerName("공연기획센터")
                .isOpen(true)
                .build();
    }

    private QueueEntry createQueueEntry(Long userId, Long concertId) {
        return new QueueEntry(userId, concertId, 1L, LocalDateTime.now());
    }

    private String userUuid() {
        return "user-uuid-123";
    }

    @Test
    void 대기열_입장_성공() {
        // Given
        Long userId = 1L;
        String userUuid = userUuid();
        Long concertId = 100L;
        UserEntity user = user(userId, userUuid);
        ConcertEntity concert = concert(concertId);

        when(userService.getUser(userUuid)).thenReturn(user);
        when(concertService.getIsAvailableConcert(concertId)).thenReturn(concert);
        when(inMemoryQueueManager.isInQueue(userId, concertId)).thenReturn(false);
        when(inMemoryQueueManager.addToQueue(userId, concertId))
                .thenReturn(createQueueEntry(userId, concertId));
        when(inMemoryQueueManager.getQueueRank(userId, concertId)).thenReturn(0L);
        when(jwtTokenProvider.createQueueToken(eq(userUuid), eq(concertId), any(Map.class)))
                .thenReturn("mock-queue-token");

        // When
        QueueTokenRequest request = new QueueTokenRequest(concertId);
        QueueTokenResponse response = queueService.enterQueue(userUuid, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getQueueToken()).isEqualTo("mock-queue-token");
        assertThat(response.getStatus()).isEqualTo(QueueStatus.ISSUED);
        assertThat(response.getRank()).isEqualTo(0L);
        assertThat(response.getEtaSeconds()).isEqualTo(0L);

        // 대기열 로그 저장 검증
        verify(queueAuditLogRepository).save(any(QueueAuditLogEntity.class));
    }

    @Test
    void 존재하지_않는_콘서트_대기열_입장_실패() {
        // Given
        String userUuid = userUuid();
        Long concertId = 999L;

        when(concertService.getIsAvailableConcert(concertId))
                .thenThrow(new ConcertNotAvailableException());

        QueueTokenRequest request = new QueueTokenRequest(concertId);

        // When & Then
        assertThatThrownBy(() -> queueService.enterQueue(userUuid, request))
                .isInstanceOf(ConcertNotAvailableException.class);
    }

    @Test
    void 이미_대기열에_등록된_사용자_중복_입장_실패() {
        // Given
        Long userId = 1L;
        String userUuid = userUuid();
        Long concertId = 100L;
        UserEntity user = user(userId, userUuid);
        ConcertEntity concert = concert(concertId);

        when(userService.getUser(userUuid)).thenReturn(user);
        when(concertService.getIsAvailableConcert(concertId)).thenReturn(concert);
        when(inMemoryQueueManager.isInQueue(userId, concertId)).thenReturn(true);

        QueueTokenRequest request = new QueueTokenRequest(concertId);

        // When & Then
        assertThatThrownBy(() -> queueService.enterQueue(userUuid, request))
                .isInstanceOf(AlreadyInQueueException.class);
    }


    // 대기열 상태 조회

    @Test
    void 대기중_상태_조회() {
        // Given
        String queueToken = "valid-queue-token";
        String userUuid = userUuid();
        Long concertId = 100L;
        Long userId = 1L;
        UserEntity user = user(userId, userUuid);

        // JWT 토큰 파싱 Mock
        Jws<Claims> mockJws = mock(Jws.class);
        Claims mockClaims = mock(Claims.class);
        when(mockJws.getBody()).thenReturn(mockClaims);
        when(mockClaims.getSubject()).thenReturn(userUuid);
        when(mockClaims.get("concertId")).thenReturn(concertId);

        when(jwtTokenProvider.parseQueue(queueToken)).thenReturn(mockJws);
        when(userService.getUser(userUuid)).thenReturn(user);
        when(inMemoryQueueManager.isActiveUser(userId, concertId)).thenReturn(false);
        when(inMemoryQueueManager.isInQueue(userId, concertId)).thenReturn(true);
        when(inMemoryQueueManager.getQueueRank(userId, concertId)).thenReturn(5L);

        // When
        QueueTokenResponse response = queueService.getQueueStatus(userUuid, queueToken);


        // Then
        assertThat(response.getQueueToken()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(QueueStatus.ISSUED);
        assertThat(response.getRank()).isEqualTo(5L);
        assertThat(response.getEtaSeconds()).isEqualTo(150L); // 5 * 30초
    }

    @Test
    void 활성화된_상태_조회() {
        // Given
        String queueToken = "active-queue-token";
        String userUuid = userUuid();
        Long concertId = 100L;
        Long userId = 1L;
        UserEntity user = user(userId, userUuid);

        // JWT 토큰 파싱 Mock
        Jws<Claims> mockJws = mock(Jws.class);
        Claims mockClaims = mock(Claims.class);
        when(mockJws.getBody()).thenReturn(mockClaims);
        when(mockClaims.getSubject()).thenReturn(userUuid);
        when(mockClaims.get("concertId")).thenReturn(concertId);

        when(jwtTokenProvider.parseQueue(queueToken)).thenReturn(mockJws);
        when(userService.getUser(userUuid)).thenReturn(user);
        when(inMemoryQueueManager.isActiveUser(userId, concertId)).thenReturn(true);

        // When
        QueueTokenResponse response = queueService.getQueueStatus(userUuid, queueToken);

        // Then
        assertThat(response.getStatus()).isEqualTo(QueueStatus.ACTIVE);
        assertThat(response.getQueueToken()).isEqualTo(queueToken);
    }

    @Test
    void 만료된_토큰_상태_조회() {
        // Given
        String queueToken = "expired-queue-token";
        String userUuid = userUuid();
        Long concertId = 100L;
        Long userId = 1L;
        UserEntity user = user(userId, userUuid);

        // JWT 토큰 파싱 Mock
        Jws<Claims> mockJws = mock(Jws.class);
        Claims mockClaims = mock(Claims.class);
        when(mockJws.getBody()).thenReturn(mockClaims);
        when(mockClaims.getSubject()).thenReturn(userUuid);
        when(mockClaims.get("concertId")).thenReturn(concertId);

        when(jwtTokenProvider.parseQueue(queueToken)).thenReturn(mockJws);
        when(userService.getUser(userUuid)).thenReturn(user);
        when(inMemoryQueueManager.isActiveUser(userId, concertId)).thenReturn(false);
        when(inMemoryQueueManager.isInQueue(userId, concertId)).thenReturn(false);

        // When
        QueueTokenResponse response = queueService.getQueueStatus(userUuid, queueToken);

        // Then
        assertThat(response.getStatus()).isEqualTo(QueueStatus.EXPIRED);
        assertThat(response.getQueueToken()).isEqualTo(queueToken);
    }

    @Test
    void 유효하지_않은_토큰으로_상태_조회_실패() {
        // Given
        String invalidToken = "invalid-token";
        String userUuid = userUuid();

        when(jwtTokenProvider.parseQueue(invalidToken))
                .thenThrow(new RuntimeException("Invalid token"));

        // When & Then
        assertThatThrownBy(() -> queueService.getQueueStatus(userUuid, invalidToken))
                .isInstanceOf(InvalidQueueTokenException.class);
    }
}