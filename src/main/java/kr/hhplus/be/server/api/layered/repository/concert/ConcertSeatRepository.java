package kr.hhplus.be.server.api.layered.repository.concert;

import kr.hhplus.be.server.api.layered.entity.concert.ConcertSeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertSeatRepository extends JpaRepository<ConcertSeatEntity, Long> {
}