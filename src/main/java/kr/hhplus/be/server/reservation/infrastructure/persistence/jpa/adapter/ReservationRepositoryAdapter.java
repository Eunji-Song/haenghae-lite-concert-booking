package kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.adapter;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.concert.ConcertDateNotFoundException;
import kr.hhplus.be.server.common.exception.concert.SeatNotFoundException;
import kr.hhplus.be.server.common.exception.reservation.*;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertSeatEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertDateJpaRepository;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertSeatJpaRepository;
import kr.hhplus.be.server.reservation.application.port.out.ReservationRepository;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.mapper.ReservationJpaMapper;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.repository.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 예약 저장, 조회, 좌석 점유 확인, 결제 후 확정 처리 담당
 */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationRepositoryAdapter implements ReservationRepository {

    private final ReservationJpaRepository reservationRepo;
    private final ConcertSeatJpaRepository seatRepo;
    private final ConcertDateJpaRepository dateRepo;
    private final Clock clock;


    @Override
    public Optional<Reservation> findById(Long id) {
        return reservationRepo.findById(id).map(ReservationJpaMapper::toDomain);
    }

    @Override
    public List<Reservation> findByUserId(Long userId) {
        return reservationRepo.findAllByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ReservationJpaMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Reservation save(Reservation reservation) {
        var entity = ReservationJpaMapper.toEntity(reservation);
        var saved = reservationRepo.save(entity);
        return ReservationJpaMapper.toDomain(saved);
    }

    // ========== [좌석 관련] ========== //

    @Override
    public boolean isSeatOccupiable(Long seatId) {
        long count = reservationRepo.countActiveOccupancy(seatId, LocalDateTime.now(clock));
        return count == 0;
    }

    @Override
    public Long resolveSeatId(Long concertId, LocalDate eventDate, int seatNo) {
        var date = dateRepo.findByConcert_IdAndEventDate(concertId, eventDate)
                .orElseThrow(ConcertDateNotFoundException::new);

        return seatRepo.findByConcertDate_IdAndSeatNo(date.getId(), seatNo)
                .map(ConcertSeatEntity::getId)
                .orElseThrow(SeatNotFoundException::new);
    }

    // ========== [결제 확정 처리] ========== //

    /**
     * 결제 성공 후 예약 확정 처리 (PENDING → CONFIRMED)
     */
    @Override
    @Transactional
    public void confirm(Long reservationId, Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);

        // 예약 조회
        var entity = reservationRepo.findById(reservationId)
                .orElseThrow(ReservationNotFoundException::new);

        // 접근 권한 검증
        if (!entity.getUser().getId().equals(userId)) {
            throw new ReservationAccessDeniedException();
        }

        // 상태 검증
        if (entity.getStatus() != ReservationStatus.PENDING) {
            switch (entity.getStatus()) {
                case CONFIRMED -> throw new ReservationAlreadyConfirmedException();
                case CANCELED -> throw new ReservationAlreadyCanceledException();
                case EXPIRED -> throw new ReservationExpiredException();
                default -> throw new InvalidReservationStatusException();
            }
        }

        // 상태 전이
        entity.confirm(now);
        reservationRepo.save(entity);
    }

    @Override
    public long findSeatPriceBySeatId(Long seatId) {
        Long price = reservationRepo.findSeatPriceBySeatId(seatId);
        if (price == null) {
            throw new SeatNotFoundException();
        }
        return price;
    }
}