package kr.hhplus.be.server.reservation.infrastructure.kafka;

import kr.hhplus.be.server.common.event.ReservationConfirmedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReservationConfirmedConsumer {

    @KafkaListener(
        topics = "reservation-confirmed",
        groupId = "reservation-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ReservationConfirmedMessage message) {
        log.info("Consumed: {}", message);
    }
}