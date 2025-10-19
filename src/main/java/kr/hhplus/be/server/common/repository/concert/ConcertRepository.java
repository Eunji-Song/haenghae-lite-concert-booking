package kr.hhplus.be.server.common.repository.concert;

import kr.hhplus.be.server.common.entity.concert.ConcertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertRepository extends JpaRepository<ConcertEntity, Long> {
}