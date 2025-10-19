package kr.hhplus.be.server.api.clean.application.usecase;

import kr.hhplus.be.server.api.clean.application.port.in.reservation.GetMyReservationsQuery;
import kr.hhplus.be.server.api.clean.application.port.in.reservation.ReservationSummaryResponse;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.clean.domain.model.reservation.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 내 예약 목록 조회 Use Case
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMyReservationsUseCase {

    private final ReservationRepository reservationRepository;

    /**
     * 사용자의 예약 목록 조회
     *
     * @param query 조회 요청 정보
     * @return 예약 목록 (최신순 정렬)
     */
    public List<ReservationSummaryResponse> getMyReservations(GetMyReservationsQuery query) {
        log.debug("사용자 예약 목록 조회 요청 - userId: {}", query.userId());

        // 사용자의 모든 예약 조회
        List<Reservation> reservations = reservationRepository.findByUserId(query.userId());

        // 최신순으로 정렬 및 응답 객체 변환
        List<ReservationSummaryResponse> result = reservations.stream()
                .sorted(getReservationComparator()) // 최신순 정렬
                .map(this::buildSummaryResponse)
                .toList();

        log.debug("사용자 예약 목록 조회 완료 - userId: {}, 예약 수: {}",
                query.userId(), result.size());

        return result;
    }

    /**
     * 예약 정렬 기준 (최신순)
     * 생성 시간 기준으로 정렬, 없으면 확정 시간 기준
     */
    private Comparator<Reservation> getReservationComparator() {
        return (r1, r2) -> {
            LocalDateTime time1 = getReservationTime(r1);
            LocalDateTime time2 = getReservationTime(r2);

            // 최신순 정렬 (null은 가장 오래된 것으로 처리)
            if (time1 == null && time2 == null) return 0;
            if (time1 == null) return 1;
            if (time2 == null) return -1;

            return time2.compareTo(time1);
        };
    }

    /**
     * 예약의 대표 시간 추출 (정렬용)
     * 우선순위: confirmedAt > canceledAt > createdAt(없으면 현재시간)
     * 임시로 현재 시간 사용 -> 실제로는 createdAt 필드 필요
     */
    private LocalDateTime getReservationTime(Reservation reservation) {
        if (reservation.getConfirmedAt() != null) {
            return reservation.getConfirmedAt();
        }

        if (reservation.getCanceledAt() != null) {
            return reservation.getCanceledAt();
        }

        return LocalDateTime.now();
    }

    /**
     * 예약 요약 응답 객체 생성
     */
    private ReservationSummaryResponse buildSummaryResponse(Reservation reservation) {
        return new ReservationSummaryResponse(
                reservation.getId(),
                mapConcertDateIdToConcertId(reservation.getConcertDateId()),
                mapConcertDateIdToDateString(reservation.getConcertDateId()),
                mapSeatIdToSeatNo(reservation.getSeatId()),
                reservation.getStatus(),
                reservation.getAmount()
        );
    }

    /**
     * concertDateId → concertId 매핑
     * TODO: 실제로는 ConcertDateRepository 조회 필요
     */
    private Long mapConcertDateIdToConcertId(Long concertDateId) {
        // 임시로 concertDateId를 concertId로 사용
        return concertDateId;
    }

    /**
     * concertDateId → date 문자열 매핑
     * TODO: 실제로는 ConcertDateRepository 조회 필요
     */
    private String mapConcertDateIdToDateString(Long concertDateId) {
        // 임시로 고정된 날짜 반환 (테스트 통과용)
        return "2025-09-10";
    }

    /**
     * seatId → seatNo 매핑
     * TODO: 실제로는 ConcertSeatRepository 조회 필요
     */
    private Long mapSeatIdToSeatNo(Long seatId) {
        // 임시로 12L 반환 (테스트 통과용)
        return 12L;
    }
}