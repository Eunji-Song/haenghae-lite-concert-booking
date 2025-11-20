package kr.hhplus.be.server.reservation.application.scheduler;

import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.repository.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpireScheduler {

    private final ReservationJpaRepository reservationJpaRepository;
    private final Clock clock;

    /**
     * 만료된(PENDING + holdExpiresAt <= now) 홀드 예약을 주기적으로 EXPIRED 처리
     *
     * fixedDelay: 이전 실행 끝난 후 30초 뒤에 다시 실행
     * (필요하면 application.yml에서 값 빼도 됨)
     */
    @Transactional
    @Scheduled(fixedDelayString = "${reservation.expire.fixedDelay-ms:30000}")
    public void expireStaledReservations() {
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = reservationJpaRepository.bulkExpireStaled(now);

        if (updated > 0) {
            log.info("[ReservationExpireScheduler] expired {} stale reservations at {}", updated, now);
        }
    }
}