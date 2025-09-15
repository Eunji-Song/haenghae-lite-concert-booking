package kr.hhplus.be.server.common.exception.reservation;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class InvalidReservationStatusException extends BaseException {
    public InvalidReservationStatusException() {
        super(ErrorCode.INVALID_RESERVATION_STATUS);
    }

    public InvalidReservationStatusException(String customMessage) {
        super(ErrorCode.INVALID_RESERVATION_STATUS, customMessage);
    }
}
