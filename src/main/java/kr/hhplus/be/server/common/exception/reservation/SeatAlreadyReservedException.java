package kr.hhplus.be.server.common.exception.reservation;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class SeatAlreadyReservedException extends BaseException {

    public SeatAlreadyReservedException() {
        super(ErrorCode.SEAT_ALREADY_RESERVED);
    }

    public SeatAlreadyReservedException(String customMessage) {
        super(ErrorCode.SEAT_ALREADY_RESERVED, customMessage);
    }

    public SeatAlreadyReservedException(Long seatId) {
        super(ErrorCode.SEAT_ALREADY_RESERVED,
                String.format("좌석 ID: %d는 이미 예약된 상태입니다", seatId));
    }
}