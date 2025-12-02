package kr.hhplus.be.server.reservation.integration;

import kr.hhplus.be.server.common.integration.BaseIntegrationTest;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertDateEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertSeatEntity;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertDateJpaRepository;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertJpaRepository;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertSeatJpaRepository;
import kr.hhplus.be.server.queue.application.service.QueueService;
import kr.hhplus.be.server.reservation.application.port.in.command.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.port.in.result.ReservationResponse;
import kr.hhplus.be.server.reservation.application.port.in.usecase.ReserveSeatUseCase;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.repository.ReservationJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 각 스레드가 독립 트랜잭션으로 동작하게
class ConcurrentSeatReservationTest extends BaseIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(ConcurrentSeatReservationTest.class);

    @Autowired private ReserveSeatUseCase reserveSeatUseCase;
    @Autowired private QueueService queueService;

    @Autowired private UserJpaRepository userRepo;
    @Autowired private ConcertJpaRepository concertRepo;
    @Autowired private ConcertDateJpaRepository dateRepo;
    @Autowired private ConcertSeatJpaRepository seatRepo;
    @Autowired private ReservationJpaRepository reservationJpaRepository;

    @Test
    @DisplayName("동시에 같은 좌석을 요청하면 정확히 한 명만 성공한다")
    void onlyOneSucceeds_whenManyRequestSameSeatConcurrently() throws Exception {
        // ---------- Arrange: 테스트 데이터 ----------
        ConcertEntity concert = concertRepo.save(ConcertEntity.builder()
                .title("title")
                .artistName("artist")
                .organizerName("org")
                .open(true)
                .build());

        LocalDate eventDate = LocalDate.now().plusDays(7);
        ConcertDateEntity date = dateRepo.save(ConcertDateEntity.builder()
                .concert(concert)
                .eventDate(eventDate)
                .venueName("venue")
                .open(true)
                .build());

        ConcertSeatEntity seat = seatRepo.save(ConcertSeatEntity.builder()
                .concertDate(date)
                .seatNo(101)
                .section("A")
                .price(50_000L)
                .build());

        final int threads = 12;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        // 유저 생성 + 토큰 발급
        List<UserEntity> users = IntStream.range(0, threads)
                .mapToObj(i -> userRepo.save(UserEntity.builder()
                        .userUuid("u-" + i)
                        .email("u" + i + "@t.com")
                        .password("{noop}p")
                        .name("user-" + i)
                        .build()))
                .toList();
        users.forEach(u -> queueService.issue(u.getUserUuid(), concert.getId()));

        // 예외 수집(어떤 예외가 났는지 확인해서 원인 파악에 도움)
        List<Throwable> errors = new ArrayList<>();

        // ---------- Act: 동시에 같은 좌석 예약 시도 ----------
        for (UserEntity u : users) {
            pool.submit(() -> {
                try {
                    start.await();

                    try {
                        ReservationResponse res = reserveSeatUseCase.reserve(
                                new ReserveSeatCommand(
                                        u.getUserUuid(), concert.getId(), eventDate, seat.getSeatNo(), 120
                                )
                        );

                        System.out.println(res);

                        if (res != null && res.reservationId() != null) {
                            successes.incrementAndGet();
                            log.info("[SUCCESS] user={}, reservationId={}", u.getUserUuid(), res.reservationId());
                        } else {
                            failures.incrementAndGet();
                            log.error("[FAIL(NULL)] user={}", u.getUserUuid());
                        }
                    } catch (Throwable t) {
                        failures.incrementAndGet();
                        synchronized (errors) {
                            errors.add(t);
                        }
                        log.error("[FAIL(EX)] user={}, ex={}", u.getUserUuid(), t.toString());
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        boolean finished = done.await(20, TimeUnit.SECONDS);
        pool.shutdownNow();

        // ---------- Assert ----------
        assertThat(finished).as("모든 작업이 제한 시간 내 끝났는지").isTrue();

        // 실제로 좌석 기준 활성 예약(PENDING/CONFIRMED)이 몇 개인지 확인
        long active = reservationJpaRepository.countActiveBySeatId(seat.getId());
        log.info("success={}, fail={}, activeReservationsByQuery={}", successes.get(), failures.get(), active);

        // 반드시 정확히 1건만 성공해야 함
        assertThat(successes.get()).as("[성공은 정확히 1건]").isEqualTo(1);
        assertThat(failures.get()).as("[나머지는 모두 실패]").isEqualTo(threads - 1);

        // 디버깅을 돕기 위해 예외 목록을 같이 출력(테스트 실패 시 콘솔에서 원인 추적)
        if (!errors.isEmpty()) {
            errors.forEach(e -> log.warn("Captured error:", e));
        }
    }
}