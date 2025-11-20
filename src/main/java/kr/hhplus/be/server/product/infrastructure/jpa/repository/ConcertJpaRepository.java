package kr.hhplus.be.server.product.infrastructure.jpa.repository;

import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertJpaRepository extends JpaRepository<ConcertEntity, Long> {}