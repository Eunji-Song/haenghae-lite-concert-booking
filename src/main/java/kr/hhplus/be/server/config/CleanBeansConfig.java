package kr.hhplus.be.server.config;

import kr.hhplus.be.server.api.clean.application.port.out.PaymentRepository;
import kr.hhplus.be.server.api.clean.application.port.out.ReservationRepository;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.payment.PaymentJpaAdapter;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.reservation.ReservationJpaAdapter;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.memory.InMemoryPaymentRepository;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.memory.InMemoryReservationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CleanBeansConfig {

    // ---------------------------
    // Reservation Repository 연결
    // ---------------------------
    @Bean
    @Profile({"default", "local"})
    public ReservationRepository reservationRepository(ReservationJpaAdapter adapter) {
        return adapter;
    }

    @Bean
    @Profile("test")
    @ConditionalOnMissingBean(ReservationRepository.class)
    public ReservationRepository inMemoryReservationRepository() {
        return new InMemoryReservationRepository();
    }

    // ---------------------------
    // Payment Repository 연결
    // ---------------------------
    @Bean
    @Profile({"default", "local"})
    public PaymentRepository paymentRepository(PaymentJpaAdapter adapter) {
        return adapter;
    }

    @Bean
    @Profile("test")
    @ConditionalOnMissingBean(PaymentRepository.class)
    public PaymentRepository inMemoryPaymentRepository() {
        return new InMemoryPaymentRepository();
    }
}