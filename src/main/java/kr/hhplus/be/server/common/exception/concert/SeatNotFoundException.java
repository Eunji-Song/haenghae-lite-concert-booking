package kr.hhplus.be.server.common.exception.concert;

import kr.hhplus.be.server.common.exception.BaseException;
import kr.hhplus.be.server.common.exception.ErrorCode;

public class SeatNotFoundException extends BaseException {

    public SeatNotFoundException() {
        super(ErrorCode.SEAT_NOT_FOUND);
    }

    public SeatNotFoundException(String customMessage) {
        super(ErrorCode.SEAT_NOT_FOUND, customMessage);
    }

    public SeatNotFoundException(Long concertDateId, Long seatNo) {
        super(ErrorCode.SEAT_NOT_FOUND,
                String.format("콘서트 날짜 ID: %d, 좌석 번호: %d에 해당하는 좌석을 찾을 수 없습니다", concertDateId, seatNo));
    }
}