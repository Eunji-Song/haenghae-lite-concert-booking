package kr.hhplus.be.server.common.exception;

public class WalletNotFoundException extends BaseException {
    public WalletNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
