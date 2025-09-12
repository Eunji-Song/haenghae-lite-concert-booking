package kr.hhplus.be.server.common.exception;

public class InvalidEmailFormatException extends BaseException {
    public InvalidEmailFormatException() {
        super(ErrorCode.INVALID_EMAIL_FORMAT);
    }
}
