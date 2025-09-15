package kr.hhplus.be.server.common.exception;

public class InvalidQueueTokenException extends BaseException {
    public InvalidQueueTokenException() {
        super(ErrorCode.INVALID_QUEUE_TOKEN);
    }
}
