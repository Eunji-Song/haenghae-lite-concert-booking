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
    private final ReservationRepository reservationRepository; // 좌석 점유 가능 여부 판단용

    public List<OpenDateResponse> getOpenDates(Long concertId) {
        List<ConcertDate> dates = dateAdapter.getOpenDates(concertId);
        return dates.stream()
                .map(d -> new OpenDateResponse(
                        d.getId(),
                        d.getConcertId(),
                        d.getEventDate(),
                        d.isOpen()
                ))
                .toList();
    }

    public List<SeatAvailabilityResponse> getSeats(Long concertId, LocalDate date) {
        List<ConcertSeat> seats = seatAdapter.findSeats(concertId, date);
        return seats.stream()
                .map(s -> new SeatAvailabilityResponse(
                        s.getId(),
                        s.getSeatNo(),
                        s.getPrice(),
                        reservationRepository.isSeatOccupiable(s.getId()) ? SeatStatus.AVAILABLE
                                : SeatStatus.HELD
                ))
                .toList();
    }
}