package kr.hhplus.be.server.common.clock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;

@TestConfiguration
public class TestClockConfig {

    /** 가변 시계(Clock 하위 타입). @Primary 붙이지 마세요! */
    @Bean
    public TestClocks.MutableClock mutableTestClock() {
        return TestClocks.mutableKst(LocalDateTime.of(2025, 1, 1, 9, 0, 0));
    }

    /** 애플리케이션이 주입받는 Clock은 이 가변 시계로 통일 */
    @Bean
    @Primary
    public java.time.Clock appClock(TestClocks.MutableClock mutableClock) {
        return mutableClock;
    }
}