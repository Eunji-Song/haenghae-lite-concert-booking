package kr.hhplus.be.server.api.layered.repository.concert;

import kr.hhplus.be.server.api.layered.entity.concert.ConcertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertRepository extends JpaRepository<ConcertEntity, Long> {
}