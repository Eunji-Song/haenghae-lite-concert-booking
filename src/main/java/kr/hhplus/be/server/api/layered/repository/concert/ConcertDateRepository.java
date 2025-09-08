package kr.hhplus.be.server.api.layered.repository.concert;

import kr.hhplus.be.server.api.layered.entity.concert.ConcertDateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertDateRepository extends JpaRepository<ConcertDateEntity, Long> {
}