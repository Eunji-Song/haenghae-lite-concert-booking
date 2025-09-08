package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.payment;

import kr.hhplus.be.server.api.clean.application.port.out.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentJpaAdapter implements PaymentRepository {
}