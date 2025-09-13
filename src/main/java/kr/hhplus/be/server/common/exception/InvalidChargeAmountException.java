package kr.hhplus.be.server.common.exception;

public class InvalidChargeAmountException extends BaseException {
    public InvalidChargeAmountException() {
        super(ErrorCode.INVALID_AMOUNT);
    }
}
