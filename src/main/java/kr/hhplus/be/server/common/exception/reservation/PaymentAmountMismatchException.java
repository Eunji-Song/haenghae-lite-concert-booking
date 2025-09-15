package kr.hhplus.be.server.common.exception.reservation;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class PaymentAmountMismatchException extends BaseException {

    public PaymentAmountMismatchException() {
        super(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    public PaymentAmountMismatchException(String customMessage) {
        super(ErrorCode.PAYMENT_AMOUNT_MISMATCH, customMessage);
    }

    public PaymentAmountMismatchException(Long expectedAmount, Long actualAmount) {
        super(ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                String.format("결제 금액이 일치하지 않습니다. 예상: %d원, 실제: %d원", expectedAmount, actualAmount));
    }
}