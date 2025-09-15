package kr.hhplus.be.server.common.exception;

public class AlreadyInQueueException extends BaseException {
    public AlreadyInQueueException() {
        super(ErrorCode.ALREADY_IN_QUEUE);
    }
}
