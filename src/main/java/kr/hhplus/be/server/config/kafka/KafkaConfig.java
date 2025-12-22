package kr.hhplus.be.server.config.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.common.event.ReservationConfirmedMessage;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    public static final String TOPIC_RESERVATION_CONFIRMED = "reservation-confirmed";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // 토픽명
    @Value("${app.kafka.topics.reservation-confirmed:reservation-confirmed}")
    private String reservationConfirmedTopic;

    /**
     * ===== Topic (선택) =====
     */
    @Bean
    public NewTopic reservationConfirmedTopic() {
        return TopicBuilder.name(reservationConfirmedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * ===== Producer =====
     * key: String
     * value: ReservationConfirmedMessage (JSON)
     */
    @Bean
    public ProducerFactory<String, ReservationConfirmedMessage> reservationConfirmedProducerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        JsonSerializer<ReservationConfirmedMessage> jsonSerializer = new JsonSerializer<>(objectMapper);
        // 헤더에 type 정보 안 붙이고 싶으면 false (취향)
        jsonSerializer.setAddTypeInfo(false);

        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, ReservationConfirmedMessage> reservationConfirmedKafkaTemplate(
            ProducerFactory<String, ReservationConfirmedMessage> reservationConfirmedProducerFactory
    ) {
        return new KafkaTemplate<>(reservationConfirmedProducerFactory);
    }

    /**
     * ===== Consumer =====
     */
    @Bean
    public ConsumerFactory<String, ReservationConfirmedMessage> reservationConfirmedConsumerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "reservation-confirmed-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // key/value deserialize
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        // ErrorHandlingDeserializer가 내부적으로 실제 deserializer를 참조
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JsonDeserializer 설정
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "kr.hhplus.be.server.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ReservationConfirmedMessage.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    
    @Bean
    public DefaultErrorHandler reservationConfirmedErrorHandler() {
        // 1초 간격으로 3번 재시도 (총 4회: 최초 1 + 재시도 3)
        return new DefaultErrorHandler(new FixedBackOff(1000L, 3L));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReservationConfirmedMessage> kafkaListenerContainerFactory(
            ConsumerFactory<String, ReservationConfirmedMessage> reservationConfirmedConsumerFactory,
            DefaultErrorHandler reservationConfirmedErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, ReservationConfirmedMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(reservationConfirmedConsumerFactory);

        factory.setCommonErrorHandler(reservationConfirmedErrorHandler);

        return factory;
    }
}