package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.reservation;

import kr.hhplus.be.server.api.clean.application.port.out.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationJpaAdapter implements ReservationRepository {
}
