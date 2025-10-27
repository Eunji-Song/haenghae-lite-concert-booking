package kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.adapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.ReservationNotFoundException;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertDateEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertSeatEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertDateJpaRepository;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertJpaRepository;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertSeatJpaRepository;
import kr.hhplus.be.server.reservation.application.port.out.ReservationRepository;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.mapper.ReservationJpaMapper;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.repository.ReservationEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationRepositoryAdapter implements ReservationRepository {

    private final ReservationEntityRepository reservationRepo;
    private final ConcertJpaRepository concertRepo;
    private final ConcertDateJpaRepository dateRepo;
    private final ConcertSeatJpaRepository seatRepo;

    @PersistenceContext
    private final EntityManager em;

    @Override
    @Transactional
    public Reservation save(Reservation d) {
        ReservationEntity e = ReservationJpaMapper.toEntity(d);
        e = e.toBuilder()
                .user(em.getReference(UserEntity.class, d.getUserId()))
                .concert(em.getReference(ConcertEntity.class, d.getConcertId()))
                .concertDate(em.getReference(ConcertDateEntity.class, d.getConcertDateId()))
                .seat(em.getReference(ConcertSeatEntity.class, d.getSeatId()))
                .build();
        return ReservationJpaMapper.toDomain(reservationRepo.save(e));
    }

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
    public boolean isSeatOccupiable(Long seatId) {
        long c = reservationRepo.countActiveOccupancy(seatId, LocalDateTime.now());
        return c == 0;
    }

    @Override
    public Long resolveSeatId(Long concertId, LocalDate eventDate, int seatNo) {
        var date = dateRepo.findByConcert_IdAndEventDate(concertId, eventDate)
                .orElseThrow(() -> new IllegalArgumentException("공연일이 존재하지 않습니다."));
        var seat = seatRepo.findByConcertDate_IdAndSeatNo(date.getId(), seatNo)
                .orElseThrow(() -> new IllegalArgumentException("좌석이 존재하지 않습니다."));
        return seat.getId();
    }

    /**
     * 결제 성공 후 예약 확정 처리 (PENDING → CONFIRMED)
     */
    @Override
    @Transactional
    public void confirm(Long reservationId, Long userId) {
        ReservationEntity entity = reservationRepo.findById(reservationId)
                .orElseThrow(ReservationNotFoundException::new);

        if (!entity.getUser().getId().equals(userId)) {
            throw new ReservationAccessDeniedException();
        }

        if (entity.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태의 예약만 확정할 수 있습니다.");
        }

        entity.confirm(LocalDateTime.now());
        reservationRepo.save(entity);
    }
}