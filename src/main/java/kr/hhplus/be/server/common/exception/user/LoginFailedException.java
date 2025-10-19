package kr.hhplus.be.server.common.exception.user;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class LoginFailedException extends BaseException {
    public LoginFailedException() {
        super(ErrorCode.LOGIN_FAILED);
    }
}
