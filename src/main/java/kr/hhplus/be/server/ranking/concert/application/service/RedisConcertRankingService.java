package kr.hhplus.be.server.ranking.concert.application.service;

import kr.hhplus.be.server.ranking.concert.domain.model.FastSoldOutConcert;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Redis ZSET 기반 콘서트 매진 랭킹 구현체.
 *
 * - 키: ranking:concert:soldout:{yyyy-MM}
 *   예) ranking:concert:soldout:2025-12
 * - member: concertId (문자열)
 * - score: soldOutAt(매진 시각, KST)의 epoch milli (작을수록 먼저 매진)
 * - TTL: 월별 키마다 약 1개월(Duration.ofDays(31)) 설정
 */
@Service
@RequiredArgsConstructor
public class RedisConcertRankingService implements ConcertRankingService {

    private static final String SOLDOUT_RANKING_KEY_PREFIX = "ranking:concert:soldout:";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Duration RANKING_TTL = Duration.ofDays(31);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void recordSoldOut(Long concertId, LocalDateTime soldOutAt) {
        if (concertId == null) {
            throw new IllegalArgumentException("concertId must not be null");
        }
        if (soldOutAt == null) {
            throw new IllegalArgumentException("soldOutAt must not be null");
        }

        // KST 기준 ZonedDateTime
        ZonedDateTime kstDateTime = soldOutAt.atZone(KST);

        // soldOutAt 이 속한 YearMonth 기준으로 랭킹 키 생성
        YearMonth month = YearMonth.from(kstDateTime);
        String key = buildMonthlyKey(month);

        // score: epoch milli (절대 시간, 정렬용)
        double score = kstDateTime.toInstant().toEpochMilli();

        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();

        // ZADD ranking:concert:soldout:2025-12 score member
        zset.add(key, concertId.toString(), score);

        // 월별 랭킹 키 TTL 설정 (약 1개월)
        redisTemplate.expire(key, RANKING_TTL);
    }

    @Override
    public List<FastSoldOutConcert> getFastSoldOutTopN(YearMonth month, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        if (month == null) {
            throw new IllegalArgumentException("month must not be null");
        }

        String key = buildMonthlyKey(month);
        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();

        // ZRANGE ranking:concert:soldout:2025-12 0 (limit-1) WITHSCORES
        Set<ZSetOperations.TypedTuple<String>> tuples =
                zset.rangeWithScores(key, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        return tuples.stream()
                .map(tuple -> {
                    String member = tuple.getValue();
                    Double score = tuple.getScore();
                    if (member == null || score == null) {
                        return null;
                    }

                    Long concertId = Long.valueOf(member);
                    Instant instant = Instant.ofEpochMilli(score.longValue());
                    // score는 절대 시간, 조회 시 KST 기준으로 다시 LocalDateTime 변환
                    LocalDateTime soldOutAt = LocalDateTime.ofInstant(instant, KST);
                    return new FastSoldOutConcert(concertId, soldOutAt);
                })
                .filter(f -> f != null)
                .toList();
    }

    /**
     * 월별 랭킹 키 생성: ranking:concert:soldout:yyyy-MM
     */
    private String buildMonthlyKey(YearMonth month) {
        return SOLDOUT_RANKING_KEY_PREFIX
                + month.getYear() + "-" + String.format("%02d", month.getMonthValue());
    }
}