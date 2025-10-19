package kr.hhplus.be.server.api.clean.infrastructure.persistence.memory;

import kr.hhplus.be.server.api.clean.application.port.out.reservation.ConcertSeatRepository;
import kr.hhplus.be.server.common.entity.concert.ConcertDateEntity;
import kr.hhplus.be.server.common.entity.concert.ConcertSeatEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConcertSeatRepository 테스트용 InMemory 구현체
 */
public class InMemoryConcertSeatRepository implements ConcertSeatRepository {

    private final Map<Long, ConcertSeatEntity> seats = new ConcurrentHashMap<>();

    // 테스트 데이터 초기화
    public InMemoryConcertSeatRepository() {
        initializeTestData();
    }

    @Override
    public Optional<ConcertSeatEntity> findByConcertDateAndSeatNo(Long concertDateId, Long seatNo) {
        return seats.values().stream()
                .filter(seat -> seat.getConcertDate().getId().equals(concertDateId))
                .filter(seat -> seat.getSeatNo().equals(seatNo))
                .findFirst();
    }

    @Override
    public List<ConcertSeatEntity> findByConcertDateId(Long concertDateId) {
        return seats.values().stream()
                .filter(seat -> seat.getConcertDate().getId().equals(concertDateId))
                .sorted((s1, s2) -> s1.getSeatNo().compareTo(s2.getSeatNo()))
                .toList();
    }

    @Override
    public List<ConcertSeatEntity> findAllByConcertDateIdOrderBySeatNoAsc(Long concertDateId) {
        return null;
    }

    // 테스트 지원 메서드들

    public void clear() {
        seats.clear();
    }

    public void addSeat(ConcertSeatEntity seat) {
        seats.put(seat.getId(), seat);
    }

    // 테스트 데이터 초기화
    private void initializeTestData() {
        // ConcertDate 참조 생성
        ConcertDateEntity mockConcertDate = ConcertDateEntity.builder()
                .id(1L)
                .build();

        // 테스트용 좌석들 생성
        for (long seatNo = 1; seatNo <= 100; seatNo++) {
            ConcertSeatEntity seat = ConcertSeatEntity.builder()
                    .id(seatNo)
                    .concertDate(mockConcertDate)
                    .seatNo(seatNo)
                    .price(50000L)
                    .section(seatNo <= 50 ? "A" : "B")
                    .build();

            seats.put(seatNo, seat);
        }
    }
}