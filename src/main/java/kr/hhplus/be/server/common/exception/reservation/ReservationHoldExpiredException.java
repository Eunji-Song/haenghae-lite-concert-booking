package kr.hhplus.be.server.common.exception.reservation;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class ReservationHoldExpiredException extends BaseException {
    public ReservationHoldExpiredException() {
        super(ErrorCode.RESERVATION_HOLD_EXPIRED);
    }
}
