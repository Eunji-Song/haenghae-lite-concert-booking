package kr.hhplus.be.server.payment.application.port.out;

/**
 * 지갑 아웃바운드 포트.
 * 멱등키로 debit/refund 중복 처리를 방지해야 한다.
 */
public interface WalletPort {

    /**
     * 지갑 차감 (결제 성공 시)
     * @param relatedPaymentId 결제 엔티티 ID (없으면 null, 저장 후 보강 가능)
     */
    void debit(Long userId, Long amount, Long relatedPaymentId, String idempotencyKey);

    /**
     * 지갑 환불/충전 (결제 실패/취소/환불 시)
     * @param relatedPaymentId 환불의 원인이 된 결제 ID (없으면 null)
     */
    void refund(Long userId, Long amount, Long relatedPaymentId, String idempotencyKey);
}