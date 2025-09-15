package kr.hhplus.be.server.common.exception.reservation;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class ReservationNotFoundException extends BaseException {

    public ReservationNotFoundException() {
        super(ErrorCode.RESERVATION_NOT_FOUND);
    }

    public ReservationNotFoundException(String customMessage) {
        super(ErrorCode.RESERVATION_NOT_FOUND, customMessage);
    }

    public ReservationNotFoundException(Long reservationId) {
        super(ErrorCode.RESERVATION_NOT_FOUND,
                String.format("예약 ID: %d를 찾을 수 없습니다", reservationId));
    }
}