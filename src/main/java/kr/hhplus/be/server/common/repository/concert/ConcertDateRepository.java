package kr.hhplus.be.server.common.repository.concert;

import jakarta.validation.constraints.NotNull;
import kr.hhplus.be.server.common.entity.concert.ConcertDateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConcertDateRepository extends JpaRepository<ConcertDateEntity, Long> {
    List<ConcertDateEntity> findByConcertIdAndIsOpenIsTrueOrderByEventDateAsc(Long concertId);
    Optional<ConcertDateEntity> findByConcertIdAndEventDate(Long concert_id, @NotNull LocalDate eventDate);

}