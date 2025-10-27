package kr.hhplus.be.server.product.domain.repository;

import kr.hhplus.be.server.product.domain.model.ConcertSeat;

import java.time.LocalDate;
import java.util.List;

public interface ConcertSeatRepository {
    List<ConcertSeat> findSeats(Long concertId, LocalDate date);
}