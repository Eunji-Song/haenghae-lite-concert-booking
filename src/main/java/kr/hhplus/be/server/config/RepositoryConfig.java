package kr.hhplus.be.server.config;

import kr.hhplus.be.server.api.clean.application.port.out.payment.PaymentRepository;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ConcertSeatRepository;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.concert.ConcertSeatJpaAdapter;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.payment.PaymentJpaAdapter;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.reservation.ReservationJpaAdapter;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.memory.InMemoryConcertSeatRepository;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.memory.InMemoryPaymentRepository;
import kr.hhplus.be.server.api.clean.infrastructure.persistence.memory.InMemoryReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Repository 구현체 환경별 설정
 * - 테스트 환경: InMemory Repository (빠른 단위 테스트)
 * - 운영/개발 환경: JPA Adapter (실제 DB 연동)
 */
@Slf4j
@Configuration
public class RepositoryConfig {

    // ============================================================
    // ReservationRepository 환경별 설정
    // ============================================================

    /**
     * 운영/개발 환경 - JPA Adapter 사용
     */
    @Bean
    @Profile({"dev", "prod", "local", "default"})
    public ReservationRepository reservationRepository(ReservationJpaAdapter jpaAdapter) {
        log.info("ReservationRepository: JPA Adapter 사용");
        return jpaAdapter;
    }

    /**
     * 테스트 환경 - InMemory Repository 사용
     */
    @Bean
    @Profile("test")
    @ConditionalOnMissingBean(ReservationRepository.class)
    public ReservationRepository testReservationRepository() {
        log.info("ReservationRepository: InMemory Repository 사용");
        return new InMemoryReservationRepository();
    }

    // ============================================================
    // PaymentRepository 환경별 설정
    // ============================================================

    /**
     * 운영/개발 환경 - JPA Adapter 사용
     */
    @Bean
    @Profile({"dev", "prod", "local", "default"})
    public PaymentRepository paymentRepository(PaymentJpaAdapter jpaAdapter) {
        log.info("PaymentRepository: JPA Adapter 사용");
        return jpaAdapter;
    }

    /**
     * 테스트 환경 - InMemory Repository 사용
     */
    @Bean
    @Profile("test")
    @ConditionalOnMissingBean(PaymentRepository.class)
    public PaymentRepository testPaymentRepository() {
        log.info("PaymentRepository: InMemory Repository 사용");
        return new InMemoryPaymentRepository();
    }

    // ============================================================
    // ConcertSeatRepository 환경별 설정
    // ============================================================

    /**
     * 운영/개발 환경 - JPA Adapter 사용 (레이어드 Repository 래핑)
     */
    @Bean
    @Profile({"dev", "prod", "local", "default"})
    public ConcertSeatRepository concertSeatRepository(ConcertSeatJpaAdapter jpaAdapter) {
        log.info("ConcertSeatRepository: JPA Adapter 사용 (레이어드 래핑)");
        return jpaAdapter;
    }

    /**
     * 테스트 환경 - InMemory Repository 사용
     */
    @Bean
    @Profile("test")
    @ConditionalOnMissingBean(ConcertSeatRepository.class)
    public ConcertSeatRepository testConcertSeatRepository() {
        log.info("ConcertSeatRepository: InMemory Repository 사용");
        return new InMemoryConcertSeatRepository();
    }
}