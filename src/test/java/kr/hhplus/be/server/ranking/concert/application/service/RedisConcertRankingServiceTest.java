package kr.hhplus.be.server.ranking.concert.application.service;

import kr.hhplus.be.server.ranking.concert.domain.model.FastSoldOutConcert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisConcertRankingServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private RedisConcertRankingService service;

    @Test
    @DisplayName("recordSoldOut: 콘서트 ID와 매진 시각(KST)을 월별 키로 ZSET에 기록하고 TTL을 설정한다")
    void recordSoldOut_success() {
        // given
        Long concertId = 42L;
        // 2025-12-01 09:00:00 KST
        LocalDateTime soldOutAt = LocalDateTime.of(2025, 12, 1, 9, 0, 0);
        long expectedEpochMillis = soldOutAt
                .atZone(KST)
                .toInstant()
                .toEpochMilli();

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // when
        service.recordSoldOut(concertId, soldOutAt);

        // then
        String expectedKey = "ranking:concert:soldout:2025-12";

        verify(redisTemplate).opsForZSet();
        verify(zSetOperations).add(expectedKey, concertId.toString(), (double) expectedEpochMillis);
        // TTL 설정 호출 여부 검증 (Duration 값 자체는 내부 상수이므로 any() 사용)
        verify(redisTemplate).expire(eq(expectedKey), any());
        verifyNoMoreInteractions(redisTemplate, zSetOperations);
    }

    @Test
    @DisplayName("recordSoldOut: concertId가 null이면 예외를 던진다")
    void recordSoldOut_nullConcertId() {
        // given
        LocalDateTime soldOutAt = LocalDateTime.now();

        // expect
        assertThatThrownBy(() -> service.recordSoldOut(null, soldOutAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("concertId");

        verifyNoInteractions(redisTemplate, zSetOperations);
    }

    @Test
    @DisplayName("recordSoldOut: soldOutAt이 null이면 예외를 던진다")
    void recordSoldOut_nullSoldOutAt() {
        // given
        Long concertId = 1L;

        // expect
        assertThatThrownBy(() -> service.recordSoldOut(concertId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("soldOutAt");

        verifyNoInteractions(redisTemplate, zSetOperations);
    }

    @Test
    @DisplayName("getFastSoldOutTopN: limit가 0 이하이면 빈 리스트를 반환한다")
    void getFastSoldOutTopN_limitZeroOrLess() {
        // when
        List<FastSoldOutConcert> result = service.getFastSoldOutTopN(YearMonth.of(2025, 12), 0);

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(redisTemplate, zSetOperations);
    }

    @Test
    @DisplayName("getFastSoldOutTopN: month가 null이면 예외를 던진다")
    void getFastSoldOutTopN_nullMonth() {
        // expect
        assertThatThrownBy(() -> service.getFastSoldOutTopN(null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("month");

        verifyNoInteractions(redisTemplate, zSetOperations);
    }

    @Test
    @DisplayName("getFastSoldOutTopN: ZSET 데이터를 FastSoldOutConcert로 매핑한다 (KST 기준)")
    void getFastSoldOutTopN_success() {
        // given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        YearMonth month = YearMonth.of(2025, 12);
        String expectedKey = "ranking:concert:soldout:2025-12";

        LocalDateTime soldOutKst = LocalDateTime.of(2025, 12, 5, 20, 30);
        long epochMillis = soldOutKst.atZone(KST).toInstant().toEpochMilli();

        @SuppressWarnings("unchecked")
        ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getValue()).thenReturn("7");
        when(tuple.getScore()).thenReturn((double) epochMillis);

        Set<ZSetOperations.TypedTuple<String>> tuples = Collections.singleton(tuple);
        when(zSetOperations.rangeWithScores(expectedKey, 0, 0)).thenReturn(tuples);

        // when
        List<FastSoldOutConcert> result = service.getFastSoldOutTopN(month, 1);

        // then
        assertThat(result).hasSize(1);
        FastSoldOutConcert fastSoldOutConcert = result.get(0);
        assertThat(fastSoldOutConcert.concertId()).isEqualTo(7L);
        assertThat(fastSoldOutConcert.soldOutAt()).isEqualTo(soldOutKst);

        verify(redisTemplate).opsForZSet();
        verify(zSetOperations).rangeWithScores(expectedKey, 0, 0);
        verifyNoMoreInteractions(redisTemplate, zSetOperations);
    }

    @Test
    @DisplayName("getFastSoldOutTopN: ZSET 결과가 비어있으면 빈 리스트를 반환한다")
    void getFastSoldOutTopN_empty() {
        // given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        YearMonth month = YearMonth.of(2025, 12);
        String expectedKey = "ranking:concert:soldout:2025-12";

        when(zSetOperations.rangeWithScores(expectedKey, 0, 9)).thenReturn(Collections.emptySet());

        // when
        List<FastSoldOutConcert> result = service.getFastSoldOutTopN(month, 10);

        // then
        assertThat(result).isEmpty();

        verify(redisTemplate).opsForZSet();
        verify(zSetOperations).rangeWithScores(expectedKey, 0, 9);
        verifyNoMoreInteractions(redisTemplate, zSetOperations);
    }
}