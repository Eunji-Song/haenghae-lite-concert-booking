package kr.hhplus.be.server.ranking.concert.application.service;

import kr.hhplus.be.server.ranking.concert.domain.model.FastSoldOutConcert;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 콘서트 "빠른 매진 랭킹" 서비스.
 *
 * - 매진이 확정된 시점에 recordSoldOut을 호출해 랭킹에 반영
 * - 특정 YearMonth 기준으로 TOP N을 조회
 */
public interface ConcertRankingService {

    /**
     * 콘서트가 매진되었을 때 랭킹에 기록한다.
     *
     * @param concertId 매진된 콘서트(또는 콘서트 날짜) ID
     * @param soldOutAt 매진이 확정된 시각(KST 기준 LocalDateTime)
     */
    void recordSoldOut(Long concertId, LocalDateTime soldOutAt);

    /**
     * 주어진 YearMonth 기준으로 "가장 빨리 매진된 콘서트" TOP N 조회.
     *
     * @param month 조회 대상 YearMonth (예: 2025-12)
     * @param limit 조회할 상위 개수 (예: 10 → TOP 10)
     * @return FastSoldOutConcert 리스트 (매진이 빠른 순서)
     */
    List<FastSoldOutConcert> getFastSoldOutTopN(YearMonth month, int limit);
}