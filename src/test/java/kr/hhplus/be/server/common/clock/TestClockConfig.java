package kr.hhplus.be.server.common.clock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * 테스트 컨텍스트에서 애플리케이션 기본 Clock(ClockConfig)을 오버라이드.
 * 필요 시 @Import(TestClockConfig.class) 로 주입.
 */
@TestConfiguration
public class TestClockConfig {

    /** 기본은 “테스트 시작 시각(KST)” 고정 */
    @Bean
    public Clock testClock() {
        return TestClocks.fixedNowKst();
    }
}