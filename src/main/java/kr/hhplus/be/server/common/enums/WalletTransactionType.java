package kr.hhplus.be.server.common.enums;

/**
 * 지갑 거래 유형
 * - CHARGE: 사용자 지갑 충전 (금액 증가)
 * - DEBIT: 결제 등으로 금액 차감
 * - REFUND: 결제 취소/환불로 금액 복원
 */
public enum WalletTransactionType {
    CHARGE,
    DEBIT,
    REFUND;

    public boolean isCredit() {
        return this == CHARGE || this == REFUND;
    }

    public boolean isDebit() {
        return this == DEBIT;
    }
}