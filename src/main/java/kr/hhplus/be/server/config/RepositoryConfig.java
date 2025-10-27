package kr.hhplus.be.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA Repository 스캔 설정
 * - 도메인별 infrastructure.jpa.repository 패키지를 명시적으로 스캔
 * - 트랜잭션 관리 활성화
 *
 * 참고:
 *  - @SpringBootApplication의 컴포넌트 스캔만으로도 동작할 수 있으나,
 *    리팩토링 후 레포지토리 위치가 흩어진 경우 명시적으로 잡아두면 안전합니다.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {
        // 공용(이관 전 레거시가 남아있다면 유지)
        "kr.hhplus.be.server.common.repository",

        // identity
        "kr.hhplus.be.server.identity.infrastructure.jpa.repository",

        // wallet
        "kr.hhplus.be.server.wallet.infrastructure.jpa.repository",

        // product
        "kr.hhplus.be.server.product.infrastructure.jpa.repository",

        // queue
        "kr.hhplus.be.server.queue.infrastructure.jpa.repository",

        // reservation (clean)
        "kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.repository",

        // payment (clean)
        "kr.hhplus.be.server.payment.infrastructure.persistence.jpa.repository"
})
public class RepositoryConfig {
}