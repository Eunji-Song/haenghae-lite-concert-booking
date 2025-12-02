package kr.hhplus.be.server.product.application.service;

import kr.hhplus.be.server.common.enums.SeatStatus;
import kr.hhplus.be.server.product.api.dto.OpenDateResponse;
import kr.hhplus.be.server.product.api.dto.SeatAvailabilityResponse;
import kr.hhplus.be.server.product.domain.model.ConcertDate;
import kr.hhplus.be.server.product.domain.model.ConcertSeat;
import kr.hhplus.be.server.product.infrastructure.jpa.adapter.ConcertDateRepositoryAdapter;
import kr.hhplus.be.server.product.infrastructure.jpa.adapter.ConcertSeatRepositoryAdapter;
import kr.hhplus.be.server.reservation.application.port.out.ReservationRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {

    private final ConcertDateRepositoryAdapter dateAdapter;
    private final ConcertSeatRepositoryAdapter seatAdapter;
    private final ReservationRepository reservationRepository;

    /**
     * 콘서트별 예약 가능 일자 목록
     * - 캐시 키: concert:openDates::<concertId>
     */
    @Cacheable(cacheNames = "concert:openDates", key = "#concertId")
    public List<OpenDateResponse> getOpenDates(Long concertId) {
        List<ConcertDate> dates = dateAdapter.getOpenDates(concertId);
        return dates.stream()
                .map(d -> new OpenDateResponse(d.getId(), d.getConcertId(), d.getEventDate(), d.isOpen()))
                .toList();
    }

    /**
     * 공연 + 날짜별 좌석 조회
     * - 캐시 키: concert:seats::<concertId>:<date>
     * - 좌석의 AVAILABLE/HELD 상태까지 포함해서 캐시 (TTL은 짧게 설정)
     */
    @Cacheable(cacheNames = "concert:seats", key = "#concertId + ':' + #date")
    public List<SeatAvailabilityResponse> getSeats(Long concertId, LocalDate date) {
        List<ConcertSeat> seats = seatAdapter.findSeats(concertId, date);
        return seats.stream()
                .map(s -> new SeatAvailabilityResponse(
                        s.getId(), s.getSeatNo(), s.getPrice(),
                        reservationRepository.isSeatOccupiable(s.getId()) ? SeatStatus.AVAILABLE : SeatStatus.HELD))
                .toList();
    }
}