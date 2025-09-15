package kr.hhplus.be.server.common.exception.reservation;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class ReservationAlreadyConfirmedException extends BaseException {
    public ReservationAlreadyConfirmedException() {
        super(ErrorCode.RESERVATION_ALREADY_CONFIRMED);
    }

}
