package kr.hhplus.be.server.common.exception.wallet;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class InvalidChargeAmountException extends BaseException {
    public InvalidChargeAmountException() {
        super(ErrorCode.INVALID_AMOUNT);
    }
}
