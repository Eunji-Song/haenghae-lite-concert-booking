package kr.hhplus.be.server.payment.application.port.out;


/**
 * 결제 단계에서 예약을 조회/검증하고,
 * 결제 성공 시 예약을 확정하는 아웃바운드 포트.
 */
public interface ReservationPort {

    /**
     * 결제 전에 예약을 로드하고 금액/상태/소유자(유저) 검증까지 수행.
     * 검증 실패 시 예외를 던진다.
     */
    ReservationForPayment getReservationForPayment(Long reservationId, String userUuid, long amount);


    /**
     * 결제 성공 후 예약 확정 처리.
     * 구현체는 상태 전이, 확정 시각 설정, 동시성(낙관락/비관락) 등을 책임진다.
     */
    void confirm(Long reservationId, String userUuid);
}