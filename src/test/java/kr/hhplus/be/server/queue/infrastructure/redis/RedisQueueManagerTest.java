package kr.hhplus.be.server.queue.infrastructure.redis;

import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.common.clock.TestClockConfig;
import kr.hhplus.be.server.common.clock.TestClocks;
import kr.hhplus.be.server.queue.domain.model.QueueEntry;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("redis-test")
@Import(TestClockConfig.class)
class RedisQueueManagerTest {

    private static final Logger log = LoggerFactory.getLogger(RedisQueueManagerTest.class);

    private static final Long CONCERT_ID = 777L;

    // RedisQueueManager 정책과 맞춰야 시간 점프가 의미 있음
    private static final Duration ACTIVE_TTL = Duration.ofMinutes(10);

    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private RedisQueueManager sut;

    // ✅ TestClockConfig에서 제공하는 타입 그대로 주입
    @Autowired private TestClocks.MutableClock mutableTestClock;

    @BeforeEach
    void setUp() {
        flushDb();
        log.info("[SETUP] clockInstant={}", mutableTestClock.instant());
    }

    @AfterEach
    void tearDown() {
        flushDb();
    }

    @Test
    @DisplayName("만료 재현: 시간 점프 후 promote()가 만료 active를 정리하고 waiting을 승급한다 (단계별 로그)")
    void expire_then_promote_should_work() {
        snapshot("START");

        // 1) ACTIVE 50 채우기
        for (int i = 1; i <= 50; i++) {
            QueueEntry e = sut.addToQueue("active-" + i, CONCERT_ID);
            if (i <= 2 || i >= 49) logEntry("ISSUE_ACTIVE_" + i, e);
            assertThat(e.status()).isEqualTo(QueueStatus.ACTIVE);
        }
        snapshot("AFTER_FILL_ACTIVE_50");

        // 2) waiting 2개
        QueueEntry w1 = sut.addToQueue("wait-1", CONCERT_ID);
        QueueEntry w2 = sut.addToQueue("wait-2", CONCERT_ID);
        logEntry("ISSUE_WAIT_1", w1);
        logEntry("ISSUE_WAIT_2", w2);

        assertThat(w1.status()).isEqualTo(QueueStatus.ISSUED);
        assertThat(w2.status()).isEqualTo(QueueStatus.ISSUED);

        snapshot("AFTER_ISSUE_WAITING_2");

        // 3) 시간 점프 (ACTIVE_TTL + 1초)
        mutableTestClock.advanceMinutes(ACTIVE_TTL.toMinutes());
        mutableTestClock.advanceSeconds(1);
        log.info("[TIME] jump -> clockInstant={}", mutableTestClock.instant());

        // 4) promote 호출 (Lua 내부에서 expired active cleanup 수행)
        long promoted = sut.promote(CONCERT_ID, 50);
        log.info("[PROMOTE] promotedCount={}", promoted);

        snapshot("AFTER_PROMOTE");

        // 5) waiting -> active 승급 확인
        QueueStatus s1 = sut.statusOf(w1.token(), w1.userUuid());
        QueueStatus s2 = sut.statusOf(w2.token(), w2.userUuid());
        log.info("[STATUS] w1={}, w2={}", s1, s2);

        assertThat(promoted).isEqualTo(2L);
        assertThat(s1).isEqualTo(QueueStatus.ACTIVE);
        assertThat(s2).isEqualTo(QueueStatus.ACTIVE);
    }

    // -------------------
    // logging / redis helper
    // -------------------

    private void logEntry(String label, QueueEntry e) {
        log.info("[ENTRY] {} -> token={}, user={}, concert={}, status={}, rank={}",
                label, e.token(), e.userUuid(), e.concertId(), e.status(), e.rank());
    }

    private void snapshot(String title) {
        String activeKey = "queue:active:" + CONCERT_ID;
        String waitingKey = "queue:waiting:" + CONCERT_ID;

        Long activeCount = redisTemplate.opsForZSet().zCard(activeKey);
        Long waitingCount = redisTemplate.opsForZSet().zCard(waitingKey);

        log.info("====================================");
        log.info("[SNAPSHOT] {} (clockInstant={})", title, mutableTestClock.instant());
        log.info("activeCount={}, waitingCount={}", activeCount, waitingCount);

        Set<ZSetOperations.TypedTuple<String>> actives =
                redisTemplate.opsForZSet().rangeWithScores(activeKey, 0, 3);
        if (actives != null && !actives.isEmpty()) {
            log.info("ACTIVE(top4) token / expireAtMillis");
            actives.forEach(t -> log.info("  - token={}, expireAt={}", t.getValue(), t.getScore()));
        }

        Set<ZSetOperations.TypedTuple<String>> waitings =
                redisTemplate.opsForZSet().rangeWithScores(waitingKey, 0, 3);
        if (waitings != null && !waitings.isEmpty()) {
            log.info("WAITING(top4) token / issuedAtMillis");
            waitings.forEach(t -> log.info("  - token={}, issuedAt={}", t.getValue(), t.getScore()));
        }

        log.info("====================================");
    }

    private void flushDb() {
        var cf = redisTemplate.getConnectionFactory();
        if (cf == null) throw new IllegalStateException("RedisConnectionFactory is null (redis 설정/의존성 확인)");
        var conn = cf.getConnection();
        try {
            conn.flushDb();
        } finally {
            conn.close();
        }
    }
}