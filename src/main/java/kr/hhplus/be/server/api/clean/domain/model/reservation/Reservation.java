package kr.hhplus.be.server.api.clean.domain.model.reservation;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 순수 도메인 모델: 예약
 * - JPA 어노테이션/엔티티 참조 없음
 * - 외래키는 id(Long)로만 보유
 * - 초기 단계: 행위(메서드) 없이 데이터 홀더 역할
 */

@Getter
@AllArgsConstructor
public class Reservation {
    private final Long id;
    private final Long userId;
    private final Long concertDateId;
    private final Long seatId;

    private final ReservationStatus status;
    private final Long amount;

    private final LocalDateTime holdExpiresAt;
    private final LocalDateTime confirmedAt;
    private final LocalDateTime canceledAt;
    private final LocalDateTime expiredAt;

    private final Long version;
}