package kr.hhplus.be.server.common.lock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.function.Supplier;

/**
 * 테스트 환경용 DistributedLockExecutor 구현체.
 * 실제 Redis 없이 동작하며, 락 없이 바로 작업을 실행
 */
@TestConfiguration
@Profile("test")
public class TestDistributedLockConfig {

    @Bean
    @Primary
    public DistributedLockExecutor testDistributedLockExecutor() {
        return new DistributedLockExecutor() {
            @Override
            public void executeWithLock(String key, long timeoutMillis, Runnable task) {
                task.run();
            }

            @Override
            public <T> T executeWithLock(String key, long timeoutMillis, Supplier<T> task) {
                return task.get();
            }
        };
    }
}

