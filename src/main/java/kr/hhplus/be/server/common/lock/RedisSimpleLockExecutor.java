package kr.hhplus.be.server.common.lock;

import kr.hhplus.be.server.common.exception.lock.DistributedLockAcquisitionException;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;


/**
 * Redis 기반 분산락 실행기 구현체.
 *
 * 특징:
 * - SETNX + TTL 기반 분산락
 * - 락 value에 소유권 토큰(ownerToken)을 저장하고, Lua 스크립트로 "내가 가진 락만" 안전하게 해제
 * - timeoutMillis 동안 스핀(재시도)하며 락 획득 시도
 */
@Slf4j
@Component
@Profile("!test")
public class RedisSimpleLockExecutor implements DistributedLockExecutor {

    private static final String LOCK_KEY_PREFIX = "lock:";

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

    // 재시도 사이에 쉬는 시간 (폴링 간격)
    private static final long RETRY_INTERVAL_MILLIS = 50L;

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
        String lockKey = LOCK_KEY_PREFIX + key;
        String ownerToken = generateOwnerToken();

        // 여기서는 timeoutMillis 를
        //  - waitTimeoutMillis: 락을 기다릴 최대 시간
        //  - leaseMillis: 락 TTL
        long waitTimeoutMillis = timeoutMillis;
        long leaseMillis = timeoutMillis;

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

            // 잠시 쉰 뒤 다시 시도 (busy wait 방지)
            try {
                Thread.sleep(RETRY_INTERVAL_MILLIS);
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
}