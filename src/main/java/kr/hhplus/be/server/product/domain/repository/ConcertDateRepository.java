package kr.hhplus.be.server.product.domain.repository;

import kr.hhplus.be.server.product.domain.model.ConcertDate;

import java.util.List;

public interface ConcertDateRepository {
    List<ConcertDate> findOpenDates(Long concertId);
}