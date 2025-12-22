package kr.hhplus.be.server.reservation.infrastructure.kafka;

import kr.hhplus.be.server.common.event.ReservationConfirmedMessage;
import kr.hhplus.be.server.config.kafka.KafkaConfig;
import kr.hhplus.be.server.reservation.application.port.out.ReservationEventOutPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReservationKafkaEventOutAdapter implements ReservationEventOutPort {

    private final KafkaTemplate<String, ReservationConfirmedMessage> kafkaTemplate;

    public ReservationKafkaEventOutAdapter(KafkaTemplate<String, ReservationConfirmedMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishReservationConfirmed(ReservationConfirmedMessage message) {
        kafkaTemplate.send(KafkaConfig.TOPIC_RESERVATION_CONFIRMED, String.valueOf(message.reservationId()), message);
    }
}