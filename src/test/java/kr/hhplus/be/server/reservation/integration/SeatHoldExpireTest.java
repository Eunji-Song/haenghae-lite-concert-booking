package kr.hhplus.be.server.reservation.integration;

import kr.hhplus.be.server.common.clock.TestClockConfig;
import kr.hhplus.be.server.common.clock.TestClocks;
import kr.hhplus.be.server.common.enums.ReservationStatus;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestClockConfig.class) // 테스트에서 사용하는 단일 Clock 주입
class SeatHoldExpireTest extends BaseIntegrationTest {

    @Autowired
    private ReserveSeatUseCase reserveSeatUseCase;
    @Autowired
    private QueueService queueService;
    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Autowired
    private UserJpaRepository userRepo;
    @Autowired
    private ConcertJpaRepository concertRepo;
    @Autowired
    private ConcertDateJpaRepository dateRepo;
    @Autowired
    private ConcertSeatJpaRepository seatRepo;


    @Autowired
    private TestClocks.MutableClock clock;

    @Test
    @DisplayName("홀드 만료 후에는 같은 좌석을 다시 예약할 수 있다")
    void seatCanBeReheld_afterHoldExpires() {
        // 0) 기본 데이터 생성
        var user = userRepo.save(UserEntity.builder()
                .userUuid("u2").email("u2@test.com").password("{noop}p").name("u2").build());

        var concert = concertRepo.save(ConcertEntity.builder()
                .title("title").artistName("artist").organizerName("org").open(true).build());

        var eventDate = LocalDate.now().plusDays(3);
        var date = dateRepo.save(ConcertDateEntity.builder()
                .concert(concert).eventDate(eventDate).venueName("v").open(true).build());

        var seat = seatRepo.save(ConcertSeatEntity.builder()
                .concertDate(date).seatNo(2).section("A").price(50_000L).build());

        // 1) 대기열 토큰 발급 + 좌석 홀드(5초 유지)
        queueService.issue(user.getUserUuid(), concert.getId());
        ReservationResponse first = reserveSeatUseCase.reserve(
                new ReserveSeatCommand(user.getUserUuid(), concert.getId(), eventDate, seat.getSeatNo(), 5)
        );
        assertThat(first.reservationId()).isNotNull();

        // 2) 테스트 시계 전진(10초) → 홀드 만료 시각 경과
        clock.advanceSeconds(10);

        // 3) (선택) 강제 만료 쿼리로 상태 정리 (PENDING → EXPIRED)
        reservationJpaRepository.forceExpire(
                first.reservationId(),
                ReservationStatus.EXPIRED,
                LocalDateTime.now(clock) // 테스트 가변 시계 기준 now
        );

        // 4) 같은 좌석 재예약 → 성공해야 함
        ReservationResponse second = reserveSeatUseCase.reserve(
                new ReserveSeatCommand(user.getUserUuid(), concert.getId(), eventDate, seat.getSeatNo(), 300)
        );
        assertThat(second.reservationId()).isNotNull();
    }
}