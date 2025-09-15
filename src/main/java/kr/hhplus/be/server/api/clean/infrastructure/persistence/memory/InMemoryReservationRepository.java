package kr.hhplus.be.server.api.clean.infrastructure.persistence.memory;

import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.clean.domain.model.reservation.Reservation;
import kr.hhplus.be.server.common.enums.ReservationStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryReservationRepository implements ReservationRepository {
    private final Map<Long, Reservation> reservations = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Reservation save(Reservation reservation) {
        // ID가 없으면 새 ID 생성
        if (reservation.getId() == null) {
            Long newId = idGenerator.getAndIncrement();
            // 새 객체 생성 (ID 포함)
            Reservation savedReservation = new Reservation(
                    newId,
                    reservation.getUserId(),
                    reservation.getConcertDateId(),
                    reservation.getSeatId(),
                    reservation.getStatus(),
                    reservation.getAmount(),
                    reservation.getHoldExpiresAt(),
                    reservation.getConfirmedAt(),
                    reservation.getCanceledAt(),
                    reservation.getVersion()
            );
            reservations.put(newId, savedReservation);
            return savedReservation;
        } else {
            // 기존 객체 업데이트
            reservations.put(reservation.getId(), reservation);
            return reservation;
        }
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    @Override
    public boolean existsActiveBySeat(Long seatId) {
        return reservations.values().stream()
                .anyMatch(reservation ->
                        reservation.getSeatId().equals(seatId) &&
                                (reservation.getStatus() == ReservationStatus.PENDING ||
                                        reservation.getStatus() == ReservationStatus.CONFIRMED)
                );
    }

    @Override
    public List<Reservation> findByUserId(Long userId) {
        return reservations.values().stream()
                .filter(reservation -> reservation.getUserId().equals(userId))
                .toList();
    }

    @Override
    public List<Reservation> findActiveBySeat(Long seatId) {
        return reservations.values().stream()
                .filter(reservation ->
                        reservation.getSeatId().equals(seatId) &&
                                (reservation.getStatus() == ReservationStatus.PENDING ||
                                        reservation.getStatus() == ReservationStatus.CONFIRMED)
                )
                .toList();
    }
}
