package kr.hhplus.be.server.common.exception.queue;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class InvalidQueueTokenException extends BaseException {
    public InvalidQueueTokenException() {
        super(ErrorCode.INVALID_QUEUE_TOKEN);
    }
}
