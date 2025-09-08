package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationStatusHistoryJpaRepository extends JpaRepository<ReservationStatusHistoryEntity, Long> {
}