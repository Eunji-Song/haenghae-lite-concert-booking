package kr.hhplus.be.server.ranking.concert.api.dto;

import java.time.LocalDateTime;

/**
 * 빠른 매진 콘서트 랭킹 응답 DTO.
 *
 * - rank: 1부터 시작하는 순위
 * - concertId: 콘서트(또는 콘서트 날짜) ID
 * - soldOutAt: 매진이 확정된 시각 (KST 기준 LocalDateTime)
 */
public record FastSoldOutConcertResponse(
        int rank,
        Long concertId,
        LocalDateTime soldOutAt
) {}