package kr.hhplus.be.server.api.clean.application.usecase;

import kr.hhplus.be.server.api.clean.application.port.in.reservation.ReservationResponse;
import kr.hhplus.be.server.api.clean.application.port.in.reservation.ReserveSeatCommand;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ConcertSeatRepository;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.clean.domain.model.reservation.Reservation;
import kr.hhplus.be.server.common.entity.concert.ConcertSeatEntity;
import kr.hhplus.be.server.common.exception.concert.SeatNotFoundException;
import kr.hhplus.be.server.common.exception.reservation.SeatAlreadyReservedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReserveSeatUseCase {

    private final ReservationRepository reservationRepository;
    private final ConcertSeatRepository concertSeatRepository;

    /**
     * 좌석 예약 (홀드)
     *
     * @param command 예약 요청 정보
     * @return 예약 결과
     * @throws SeatNotFoundException 좌석을 찾을 수 없는 경우
     * @throws SeatAlreadyReservedException 이미 예약된 좌석인 경우
     * @throws IllegalArgumentException 잘못된 날짜 형식인 경우
     */
    @Transactional
    public ReservationResponse reserveSeat(ReserveSeatCommand command) {
        log.info("좌석 예약 요청 - userId: {}, concertId: {}, date: {}, seatNo: {}",
                command.userId(), command.concertId(), command.date(), command.seatNo());

        // 날짜 형식 검증 및 콘서트 날짜 ID 조회
        Long concertDateId = validateAndGetConcertDateId(command.concertId(), command.date());

        // 좌석 존재 여부 확인
        ConcertSeatEntity seat = findSeatOrThrow(concertDateId, command.seatNo());

        // 좌석 예약 가능 여부 확인
        validateSeatAvailability(seat.getId());

        // 예약 모델 객체 생성
        Reservation reservation = Reservation.createPendingReservation(
                command.userId(),
                concertDateId,
                seat.getId(),
                seat.getPrice()
        );

        // 예약 확정
        Reservation savedReservation = reservationRepository.save(reservation);

        log.info("좌석 예약 완료 - reservationId: {}, holdExpiresAt: {}",
                savedReservation.getId(), savedReservation.getHoldExpiresAt());

        return new ReservationResponse(
                savedReservation.getId(),
                savedReservation.getHoldExpiresAt()
        );
    }

    /**
     * 날짜 형식 검증 및 콘서트 날짜 ID 조회
     * 실제로는 ConcertDateRepository를 통해 조회해야 하지만,
     * 현재는 간단히 검증만 수행
     */
    private Long validateAndGetConcertDateId(Long concertId, String dateString) {
        try {
            LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            // TODO: 실제로는 ConcertDateRepository로 concertId + date 조회
            // 현재는 임시로 concertId를 concertDateId로 사용
            return concertId;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("잘못된 날짜 형식입니다. yyyy-MM-dd 형식을 사용해주세요: " + dateString);
        }
    }

    /**
     * 좌석 조회 및 존재 여부 확인
     */
    private ConcertSeatEntity findSeatOrThrow(Long concertDateId, Long seatNo) {
        return concertSeatRepository.findByConcertDateAndSeatNo(concertDateId, seatNo)
                .orElseThrow(() -> new SeatNotFoundException(concertDateId, seatNo));
    }

    /**
     * 좌석 예약 가능 여부 확인
     */
    private void validateSeatAvailability(Long seatId) {
        if (reservationRepository.existsActiveBySeat(seatId)) {
            throw new SeatAlreadyReservedException(seatId);
        }
    }
}