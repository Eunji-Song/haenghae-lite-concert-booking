package kr.hhplus.be.server.common.exception.reservation;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 예약이 만료된 상태일 때 발생하는 예외.
 * 예: 홀드 시간이 지나 결제 또는 확정이 불가능한 경우.
 */
public class ReservationExpiredException extends BaseException {

    public ReservationExpiredException() {
        super(ErrorCode.RESERVATION_EXPIRED);
    }

    public ReservationExpiredException(String message) {
        super(ErrorCode.RESERVATION_EXPIRED, message);
    }
}