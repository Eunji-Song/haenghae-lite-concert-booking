package kr.hhplus.be.server.api.clean.application.usecase;

import kr.hhplus.be.server.api.clean.infrastructure.persistence.memory.InMemoryPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentIntegrationTest {
    @Autowired
    private PayForReservationUseCase useCase;

    @Autowired
    private InMemoryPaymentRepository paymentRepository;
}