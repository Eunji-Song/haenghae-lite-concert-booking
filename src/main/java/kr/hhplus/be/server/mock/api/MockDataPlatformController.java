package kr.hhplus.be.server.mock.api;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import kr.hhplus.be.server.payment.application.event.PaymentCompletedEvent;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/mock/data-platform")
@Slf4j
public class MockDataPlatformController {

    @PostMapping("/payments/completed")
    public ResponseEntity<Void> receivePaymentCompleted(
            @RequestBody PaymentCompletedEvent event
    ) {
        log.info(
            "[MOCK-DATA-PLATFORM] paymentId={}, reservationId={}, user={}, amount={}, occurredAt={}",
            event.paymentId(),
            event.reservationId(),
            event.userUuid(),
            event.amount(),
            event.occurredAt()
        );
        return ResponseEntity.ok().build();
    }
}