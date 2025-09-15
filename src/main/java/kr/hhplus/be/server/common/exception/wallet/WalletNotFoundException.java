package kr.hhplus.be.server.common.exception.wallet;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class WalletNotFoundException extends BaseException {
    public WalletNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
