package kr.hhplus.be.server.common.exception.user;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class InvalidEmailFormatException extends BaseException {
    public InvalidEmailFormatException() {
        super(ErrorCode.INVALID_EMAIL_FORMAT);
    }
}
