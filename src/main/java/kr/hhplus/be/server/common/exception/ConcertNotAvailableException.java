package kr.hhplus.be.server.common.exception;

public class ConcertNotAvailableException extends BaseException {
    public ConcertNotAvailableException() {
        super(ErrorCode.CONCERT_NOT_AVAILABLE);
    }
}
