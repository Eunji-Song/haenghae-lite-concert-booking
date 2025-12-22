package kr.hhplus.be.server.reservation.infrastructure.kafka;

import kr.hhplus.be.server.common.event.ReservationConfirmedMessage;
import kr.hhplus.be.server.config.kafka.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationConfirmedProducer {

    private final KafkaTemplate<String, ReservationConfirmedMessage> reservationConfirmedKafkaTemplate;

    public void send(ReservationConfirmedMessage message) {
        String key = String.valueOf(message.reservationId());

        reservationConfirmedKafkaTemplate.send(KafkaConfig.TOPIC_RESERVATION_CONFIRMED, key, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] reservation.confirmed send FAILED. key={}, payload={}", key, message, ex);
                        return;
                    }
                    log.info("[Kafka] reservation.confirmed send OK. topic={}, partition={}, offset={}, key={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            key
                    );
                });
    }
}