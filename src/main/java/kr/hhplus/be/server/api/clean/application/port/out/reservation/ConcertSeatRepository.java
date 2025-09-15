package kr.hhplus.be.server.api.clean.application.port.out.reservation;

import kr.hhplus.be.server.common.entity.concert.ConcertSeatEntity;

import java.util.List;
import java.util.Optional;

public interface ConcertSeatRepository {

    /**
     * 콘서트 날짜와 좌석 번호로 좌석 조회
     */
    Optional<ConcertSeatEntity> findByConcertDateAndSeatNo(Long concertDateId, Long seatNo);

    /**
     * 콘서트 날짜별 모든 좌석 조회
     */
    List<ConcertSeatEntity> findByConcertDateId(Long concertDateId);

    List<ConcertSeatEntity> findAllByConcertDateIdOrderBySeatNoAsc(Long concertDateId);

}