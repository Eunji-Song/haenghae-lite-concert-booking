package kr.hhplus.be.server.common.exception.reservation;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class ReservationAccessDeniedException extends BaseException {

    public ReservationAccessDeniedException() {
        super(ErrorCode.RESERVATION_ACCESS_DENIED);
    }

    public ReservationAccessDeniedException(String customMessage) {
        super(ErrorCode.RESERVATION_ACCESS_DENIED, customMessage);
    }

    public ReservationAccessDeniedException(Long userId, Long reservationId) {
        super(ErrorCode.RESERVATION_ACCESS_DENIED,
                String.format("사용자 ID: %d는 예약 ID: %d에 대한 권한이 없습니다", userId, reservationId));
    }
}