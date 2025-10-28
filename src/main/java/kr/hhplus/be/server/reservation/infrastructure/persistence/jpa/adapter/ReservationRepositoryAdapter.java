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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
                .stream().map(ReservationJpaMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Reservation save(Reservation reservation) {
        try {
            var entity = ReservationJpaMapper.toEntity(reservation);
            var saved = reservationRepo.save(entity);
            return ReservationJpaMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            // uk_resv_seat_active 위반 → 이미 누군가 활성 점유 중
            throw new SeatAlreadyReservedException();
        }
    }

    @Override
    public boolean isSeatOccupiable(Long seatId) {
        long count = reservationRepo.countActiveBySeatId(seatId);
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

    @Override
    @Transactional
    public void confirm(Long reservationId, Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        var entity = reservationRepo.findById(reservationId)
                .orElseThrow(ReservationNotFoundException::new);

        if (!entity.getUser().getId().equals(userId)) {
            throw new ReservationAccessDeniedException();
        }
        if (entity.getStatus() != ReservationStatus.PENDING) {
            switch (entity.getStatus()) {
                case CONFIRMED -> throw new ReservationAlreadyConfirmedException();
                case CANCELED -> throw new ReservationAlreadyCanceledException();
                case EXPIRED -> throw new ReservationExpiredException();
                default -> throw new InvalidReservationStatusException();
            }
        }
        entity.confirm(now);            // 상태 전이 + isActive=true 유지
        reservationRepo.save(entity);
    }

    @Override
    @Transactional
    public void lockSeat(Long seatId) {
        seatRepo.lockById(seatId);
    }

    @Override
    public long findSeatPriceBySeatId(Long seatId) {
        Long price = seatRepo.findPriceById(seatId);
        if (price == null) {
            throw new SeatNotFoundException();
        }
        return price;
    }
}