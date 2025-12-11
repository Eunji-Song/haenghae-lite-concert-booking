package kr.hhplus.be.server.common.lock;

import kr.hhplus.be.server.common.exception.lock.DistributedLockAcquisitionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Redis 기반 분산락 실행기 구현체.
 *
 * 특징:
 * - SETNX + TTL 기반 분산락
 * - 락 value에 소유권 토큰(ownerToken)을 저장하고, Lua 스크립트로 "내가 가진 락만" 안전하게 해제
 * - timeoutMillis 동안 랜덤 백오프를 사용해 스핀(재시도)하며 락 획득 시도
 */
@Slf4j
@Component
@Profile("!test")
public class RedisSimpleLockExecutor implements DistributedLockExecutor {

    private static final String LOCK_KEY_PREFIX = "lock:";

    /**
     * 외부에서 넘겨주는 key 형식 검증용 패턴.
     * (도메인에서 사용하는 reservation:seat:..., wallet:... 등의 형태를 허용)
     */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9:_\\-]+$");

    /**
     * 소유권 검증 후 삭제하는 Lua 스크립트:
     * if get(key) == ownerToken then del(key) end
     */
    private static final String UNLOCK_LUA = """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        else
            return 0
        end
        """;

    // 랜덤 백오프 범위 (밀리초)
    private static final long MIN_RETRY_INTERVAL_MILLIS = 30L;
    private static final long MAX_RETRY_INTERVAL_MILLIS = 100L;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> unlockScript;

    public RedisSimpleLockExecutor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.unlockScript = new DefaultRedisScript<>(UNLOCK_LUA, Long.class);
    }

    @Override
    public void executeWithLock(String key, long timeoutMillis, Runnable task) {
        executeWithLock(key, timeoutMillis, () -> {
            task.run();
            return null;
        });
    }

    @Override
    public <T> T executeWithLock(String key, long timeoutMillis, Supplier<T> task) {
        // 1) public API 진입 시 입력값 검증
        validateKey(key);
        long effectiveTimeout = validateTimeout(timeoutMillis);

        String lockKey = LOCK_KEY_PREFIX + key;
        String ownerToken = generateOwnerToken();

        // 여기서는 timeoutMillis 를
        //  - waitTimeoutMillis: 락을 기다릴 최대 시간
        //  - leaseMillis      : 락 TTL
        long waitTimeoutMillis = effectiveTimeout;
        long leaseMillis = effectiveTimeout;

        boolean acquired = tryAcquireWithWait(lockKey, ownerToken, waitTimeoutMillis, leaseMillis);
        if (!acquired) {
            throw new DistributedLockAcquisitionException(
                    "Failed to acquire lock within timeout: " + lockKey
            );
        }

        try {
            return task.get();
        } finally {
            safeRelease(lockKey, ownerToken);
        }
    }

    /**
     * 락 소유자를 나타내는 토큰 생성.
     * - UUID + Thread ID 조합으로 유니크하게 생성.
     */
    private String generateOwnerToken() {
        return UUID.randomUUID() + ":" + Thread.currentThread().getId();
    }

    /**
     * waitTimeoutMillis 동안 재시도(스핀) 하며 락 획득을 시도.
     * - 각 시도 사이에는 [MIN, MAX] 범위의 랜덤 시간 동안 대기 → 동시 충돌 완화
     */
    private boolean tryAcquireWithWait(String lockKey,
                                       String ownerToken,
                                       long waitTimeoutMillis,
                                       long leaseMillis) {

        long deadline = System.currentTimeMillis() + waitTimeoutMillis;

        while (System.currentTimeMillis() < deadline) {
            try {
                Boolean result = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, ownerToken, Duration.ofMillis(leaseMillis));

                if (Boolean.TRUE.equals(result)) {
                    // 락 획득 성공
                    return true;
                }
            } catch (DataAccessException e) {
                throw new DistributedLockAcquisitionException(
                        "Redis error while acquiring lock: " + lockKey, e
                );
            }

            // 남은 시간 내에서 랜덤 백오프
            long now = System.currentTimeMillis();
            long remaining = deadline - now;
            if (remaining <= 0) {
                break;
            }

            long sleepMillis = Math.min(nextBackoffMillis(), remaining);
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DistributedLockAcquisitionException(
                        "Interrupted while waiting for lock: " + lockKey, e
                );
            }
        }

        // 대기 시간 동안 락을 잡지 못함
        return false;
    }

    /**
     * Lua 스크립트를 사용해 "내가 가진 락"인 경우에만 안전하게 unlock.
     */
    private void safeRelease(String lockKey, String ownerToken) {
        try {
            redisTemplate.execute(
                    unlockScript,
                    Collections.singletonList(lockKey),
                    ownerToken
            );
        } catch (DataAccessException e) {
            log.warn("Failed to release lockKey={} owner={}", lockKey, ownerToken, e);
        }
    }

    /**
     * 캐시 키 유효성 검증.
     * - null/blank 금지
     * - 허용된 문자 집합(알파벳/숫자/:/_/-)만 사용
     */
    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Lock key must not be null or blank");
        }
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("Lock key contains invalid characters: " + key);
        }
    }

    /**
     * timeoutMillis 유효성 검증.
     * - 0 이하 금지
     * 필요하다면 상한 값도 두고 싶으면 여기서 함께 체크할 수 있음.
     */
    private long validateTimeout(long timeoutMillis) {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be greater than 0: " + timeoutMillis);
        }
        return timeoutMillis;
    }

    /**
     * [MIN_RETRY_INTERVAL_MILLIS, MAX_RETRY_INTERVAL_MILLIS] 범위의 랜덤 backoff 생성.
     */
    private long nextBackoffMillis() {
        if (MIN_RETRY_INTERVAL_MILLIS == MAX_RETRY_INTERVAL_MILLIS) {
            return MIN_RETRY_INTERVAL_MILLIS;
        }
        return ThreadLocalRandom.current()
                .nextLong(MIN_RETRY_INTERVAL_MILLIS, MAX_RETRY_INTERVAL_MILLIS + 1);
    }
}