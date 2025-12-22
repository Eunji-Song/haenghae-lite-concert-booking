package kr.hhplus.be.server.payment.application.event;

import java.time.Instant;

/**
 * 결제 완료 이벤트
 *
 * - 결제 트랜잭션이 성공적으로 커밋된 이후 발행되는 이벤트
 * - 결제 도메인의 상태 변경(결제 완료)을 외부 관심사에 전달하기 위한 목적
 * - 이벤트 리스너를 통해 데이터 플랫폼 전송, 알림, 통계 집계 등의 부가 로직을 트랜잭션과 분리하여 처리
 * 
 */
public record PaymentCompletedEvent(

        Long paymentId, // 결제 식별자
        
        Long reservationId, // 결제가 발생한 예약 식별자
        
        String userUuid, // 결제 사용자 UUID

        Long concertId, // 결제가 발생한 콘서트 식별자

        Long amount, // 결제 금액

        String idempotencyKey, // 멱등성 보장을 위한 키

        /**
         * 이벤트 발생 시각 (UTC 기준)
         * - 시스템 간 시간 불일치 문제를 방지하기 위해 Instant 사용
         * - 로그, 외부 시스템 전송, 이벤트 추적에 사용
         */
        Instant occurredAt) {
}