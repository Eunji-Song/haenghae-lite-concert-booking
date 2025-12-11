package kr.hhplus.be.server.common.lock;

import java.util.function.Supplier;

/**
 * Redis 기반 분산락 실행기.
 * - key 기준으로 락을 잡고, 주어진 작업을 실행
 * - 락을 못 잡으면 예외를 던지고 비즈니스 로직은 실행되지 않음
 */
public interface DistributedLockExecutor {

    /**
     * 락을 잡은 뒤, Runnable 작업을 실행한다.
     */
    void executeWithLock(String key, long timeoutMillis, Runnable task);

    /**
     * 락을 잡은 뒤, Supplier 작업을 실행하고 결과를 반환한다.
     */
    <T> T executeWithLock(String key, long timeoutMillis, Supplier<T> task);
}