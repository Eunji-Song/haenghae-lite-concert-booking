package kr.hhplus.be.server.common.exception.reservation;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class ReservationAlreadyCanceledException extends BaseException {
    public ReservationAlreadyCanceledException() {
        super(ErrorCode.RESERVATION_ALREADY_CANCELED);
    }
}
