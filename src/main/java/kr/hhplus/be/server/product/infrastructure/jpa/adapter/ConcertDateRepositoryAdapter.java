package kr.hhplus.be.server.product.infrastructure.jpa.adapter;

import kr.hhplus.be.server.product.domain.model.ConcertDate;
import kr.hhplus.be.server.product.infrastructure.jpa.mapper.ProductJpaMapper;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertDateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertDateRepositoryAdapter {

    private final ConcertDateJpaRepository dateRepo;

    public List<ConcertDate> getOpenDates(Long concertId) {
        return dateRepo.findAllByConcert_IdAndOpenTrueOrderByEventDateAsc(concertId)
                .stream().map(ProductJpaMapper::toDomain).toList();
    }

    public Long getDateId(Long concertId, LocalDate eventDate) {
        return dateRepo.findIdByConcertIdAndEventDate(concertId, eventDate)
                .orElse(null);
    }
}