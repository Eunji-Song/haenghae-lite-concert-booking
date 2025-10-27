package kr.hhplus.be.server.product.infrastructure.jpa.adapter;

import kr.hhplus.be.server.product.domain.model.ConcertSeat;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertSeatJpaRepository;
import kr.hhplus.be.server.product.infrastructure.jpa.mapper.ProductJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertSeatRepositoryAdapter {

    private final ConcertSeatJpaRepository seatRepo;

    public List<ConcertSeat> findSeats(Long concertId, LocalDate eventDate) {
        return seatRepo.findAllByConcertIdAndEventDate(concertId, eventDate)
                .stream().map(ProductJpaMapper::toDomain).toList();
    }
}