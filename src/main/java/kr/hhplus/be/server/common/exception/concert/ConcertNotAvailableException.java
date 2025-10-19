package kr.hhplus.be.server.common.exception.concert;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class ConcertNotAvailableException extends BaseException {
    public ConcertNotAvailableException() {
        super(ErrorCode.CONCERT_NOT_AVAILABLE);
    }
}
