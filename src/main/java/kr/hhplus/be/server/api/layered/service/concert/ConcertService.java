package kr.hhplus.be.server.api.layered.service.concert;

import kr.hhplus.be.server.api.layered.entity.concert.ConcertEntity;
import kr.hhplus.be.server.api.layered.repository.concert.ConcertRepository;
import kr.hhplus.be.server.common.exception.ConcertNotAvailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {
    private final ConcertRepository concertRepository;

    public ConcertEntity getIsAvailableConcert(Long concertId) {
        return concertRepository.findById(concertId).orElseThrow(ConcertNotAvailableException::new);
    }
}
