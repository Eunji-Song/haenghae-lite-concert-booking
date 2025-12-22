package kr.hhplus.be.server.mock.kafka;

import kr.hhplus.be.server.common.event.ReservationConfirmedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReservationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventConsumer.class);

    @KafkaListener(topics = "reservation-events", groupId = "data-platform")
    public void consume(ReservationConfirmedMessage message) {
        log.info("[DATA-PLATFORM MOCK] reservation confirmed message received: {}", message);
    }
}