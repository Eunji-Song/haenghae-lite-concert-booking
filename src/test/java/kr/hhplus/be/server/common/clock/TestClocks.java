package kr.hhplus.be.server.common.clock;

import java.time.*;
import java.time.temporal.ChronoUnit;

/**
 * 테스트 전용 Clock 유틸리티.
 * - KST(Asia/Seoul) 기준 고정/가변 시계 제공
 */
public final class TestClocks {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private TestClocks() {}

    /** KST 기준 고정 시계 (yyyy-MM-ddTHH:mm:ss 지정) */
    public static Clock fixedKst(LocalDateTime kstDateTime) {
        Instant instant = kstDateTime.atZone(KST).toInstant();
        return Clock.fixed(instant, KST);
    }

    /** “지금”을 KST로 고정 */
    public static Clock fixedNowKst() {
        return fixedKst(LocalDateTime.now(KST).truncatedTo(ChronoUnit.SECONDS));
    }

    /** 가변 시계: 테스트 중 시간이 흐르도록 advance 가능 */
    public static MutableClock mutableKst(LocalDateTime kstDateTime) {
        return new MutableClock(kstDateTime.atZone(KST).toInstant(), KST);
    }

    /** 가변 Clock 구현 */
    public static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return new MutableClock(instant, zone); }
        @Override public Instant instant() { return instant; }

        public void set(LocalDateTime kstDateTime) {
            this.instant = kstDateTime.atZone(zone).toInstant();
        }
        public void advanceSeconds(long seconds) { instant = instant.plusSeconds(seconds); }
        public void advanceMinutes(long minutes) { instant = instant.plusSeconds(minutes * 60); }
        public void advanceHours(long hours)     { instant = instant.plusSeconds(hours * 3600); }
    }
}