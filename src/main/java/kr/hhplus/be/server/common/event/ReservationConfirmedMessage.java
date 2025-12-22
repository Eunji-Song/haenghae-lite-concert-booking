package kr.hhplus.be.server.common.event;

import java.time.LocalDateTime;

/**
 * 예약 확정 이벤트 (Kafka 발행용)
 *
 * - 결제 성공 + 예약 확정이 완료되었음을 외부로 알리는 이벤트
 * - 내부 도메인 상세(seat, date)는 포함하지 않음
 */
public record ReservationConfirmedMessage(
        Long reservationId,
        Long userId,
        Long concertId,
        Long amount,
        LocalDateTime confirmedAt
) {
}