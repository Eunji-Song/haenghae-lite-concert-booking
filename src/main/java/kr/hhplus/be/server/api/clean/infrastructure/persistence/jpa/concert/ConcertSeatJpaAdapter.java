package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.concert;

import kr.hhplus.be.server.api.clean.application.port.out.reservation.ConcertSeatRepository;
import kr.hhplus.be.server.common.entity.concert.ConcertSeatEntity;
import kr.hhplus.be.server.common.repository.concert.ConcertSeatEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ConcertSeatRepository의 JPA 구현체
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ConcertSeatJpaAdapter implements ConcertSeatRepository {

    private final ConcertSeatEntityRepository concertSeatRepository;

    @Override
    public Optional<ConcertSeatEntity> findByConcertDateAndSeatNo(Long concertDateId, Long seatNo) {
        log.debug("좌석 조회 - concertDateId: {}, seatNo: {}", concertDateId, seatNo);

        return concertSeatRepository.findByConcertDateIdAndSeatNo(concertDateId, seatNo);
    }

    @Override
    public List<ConcertSeatEntity> findByConcertDateId(Long concertDateId) {
        log.debug("콘서트 날짜별 좌석 목록 조회 - concertDateId: {}", concertDateId);

        return concertSeatRepository.findByConcertDateIdOrderBySeatNoAsc(concertDateId);
    }

    @Override
    public List<ConcertSeatEntity> findAllByConcertDateIdOrderBySeatNoAsc(Long concertDateId) {
        return null;
    }
}