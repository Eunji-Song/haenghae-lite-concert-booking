package kr.hhplus.be.server.api.layered.service.concert;

import kr.hhplus.be.server.api.clean.application.port.out.reservation.ConcertSeatRepository;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.layered.dto.concert.OpenDateResponse;
import kr.hhplus.be.server.api.layered.dto.concert.SeatAvailabilityResponse;
import kr.hhplus.be.server.common.entity.concert.ConcertDateEntity;
import kr.hhplus.be.server.common.entity.concert.ConcertEntity;
import kr.hhplus.be.server.common.repository.concert.ConcertDateRepository;
import kr.hhplus.be.server.common.repository.concert.ConcertRepository;
import kr.hhplus.be.server.common.exception.concert.ConcertNotAvailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final ConcertDateRepository concertDateRepository;
    private final ConcertSeatRepository concertSeatRepository;
    private final ReservationRepository reservationRepository;

    public ConcertEntity getIsAvailableConcert(Long concertId) {
        return concertRepository.findById(concertId)
                .orElseThrow(ConcertNotAvailableException::new);
    }

    // 예약 가능 날짜 조회
    public List<OpenDateResponse> getOpenDates(Long concertId) {
        getIsAvailableConcert(concertId);

        return concertDateRepository.findByConcertIdAndIsOpenIsTrueOrderByEventDateAsc(concertId).stream()
                .map(cd -> new OpenDateResponse(
                        cd.getId(),
                        cd.getConcert().getId(),
                        cd.getEventDate(),
                        cd.getIsOpen()
                ))
                .toList();
    }

    /**
     * 특정 날짜의 예약 가능 좌석 조회
     * @param date "yyyy-MM-dd"
     */
    public List<SeatAvailabilityResponse> getAvailableSeatList(Long concertId, String date) {

        // 0) date 문자열 → LocalDate 파싱
        final LocalDate eventDate;
        try {
            eventDate = LocalDate.parse(date, DateTimeFormatter.ISO_DATE); // "yyyy-MM-dd"
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("잘못된 날짜 형식입니다. yyyy-MM-dd 형식을 사용해주세요: " + date);
        }

        // 1) 날짜 엔티티 찾기
        ConcertDateEntity concertDate = concertDateRepository.findByConcertIdAndEventDate(concertId, eventDate)
                .orElseThrow(() -> new IllegalArgumentException("잘못된 콘서트/날짜입니다."));

        // 2) 해당 날짜의 전체 좌석 조회
        var seats = concertSeatRepository.findAllByConcertDateIdOrderBySeatNoAsc(concertDate.getId());

        // 3) 좌석별 가용성 계산
        var now = LocalDateTime.now();
        return seats.stream()
                .map(seat -> {
                    boolean inUse = reservationRepository.existsActiveBySeat(seat.getId());
                    return inUse
                            ? SeatAvailabilityResponse.unavailable(seat.getId(), seat.getSeatNo(), seat.getPrice())
                            : SeatAvailabilityResponse.available(seat.getId(), seat.getSeatNo(), seat.getPrice());
                })
                .toList();
    }
}