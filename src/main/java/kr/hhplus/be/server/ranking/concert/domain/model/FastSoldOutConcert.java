package kr.hhplus.be.server.ranking.concert.domain.model;

import java.time.LocalDateTime;

/**
 * 빠르게 매진된 콘서트 정보를 나타내는 도메인 모델.
 * - concertId: 콘서트(또는 콘서트 날짜) 식별자
 * - soldOutAt: 매진이 확정된 시각 
 */
public record FastSoldOutConcert(
    Long concertId,
    LocalDateTime soldOutAt
) {}