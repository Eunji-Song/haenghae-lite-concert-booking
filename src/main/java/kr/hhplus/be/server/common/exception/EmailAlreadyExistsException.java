package kr.hhplus.be.server.common.exception;

public class EmailAlreadyExistsException extends BaseException {
    public EmailAlreadyExistsException() {
        super(ErrorCode.EMAIL_ALREADY_EXISTS);
    }
}