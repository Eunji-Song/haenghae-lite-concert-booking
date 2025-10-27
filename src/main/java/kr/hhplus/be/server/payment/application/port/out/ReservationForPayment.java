package kr.hhplus.be.server.payment.application.port.out;


/** 결제 단계에서 필요한 최소 예약 정보 프로젝션 */
public record ReservationForPayment(
        Long reservationId,
        Long userId,
        Long concertId,
        Long amount
) {}