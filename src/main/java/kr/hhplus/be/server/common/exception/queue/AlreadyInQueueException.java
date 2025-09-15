package kr.hhplus.be.server.common.exception.queue;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class AlreadyInQueueException extends BaseException {
    public AlreadyInQueueException() {
        super(ErrorCode.ALREADY_IN_QUEUE);
    }
}
