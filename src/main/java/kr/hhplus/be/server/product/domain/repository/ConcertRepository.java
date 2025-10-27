package kr.hhplus.be.server.product.domain.repository;

import kr.hhplus.be.server.product.domain.model.Concert;

import java.util.Optional;

public interface ConcertRepository {
    Optional<Concert> findById(Long id);
}