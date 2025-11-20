package kr.hhplus.be.server.common.exception.concert;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 공연 날짜(concert_dates)가 존재하지 않을 때 발생.
 */
public class ConcertDateNotFoundException extends BaseException {

    public ConcertDateNotFoundException() {
        super(ErrorCode.CONCERT_DATE_NOT_FOUND);
    }

    public ConcertDateNotFoundException(String message) {
        super(ErrorCode.CONCERT_DATE_NOT_FOUND, message);
    }
}