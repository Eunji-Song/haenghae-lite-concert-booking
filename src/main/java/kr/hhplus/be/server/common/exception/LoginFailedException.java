package kr.hhplus.be.server.common.exception;

public class LoginFailedException extends BaseException {
    public LoginFailedException() {
        super(ErrorCode.LOGIN_FAILED);
    }
}
