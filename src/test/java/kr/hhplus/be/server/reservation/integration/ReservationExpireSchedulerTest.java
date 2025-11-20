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
import kr.hhplus.be.server.queue.domain.model.QueueEntry;
import kr.hhplus.be.server.reservation.application.port.in.command.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.port.in.result.ReservationResponse;
import kr.hhplus.be.server.reservation.application.port.in.usecase.ReserveSeatUseCase;
import kr.hhplus.be.server.reservation.application.scheduler.ReservationExpireScheduler;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.repository.ReservationJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestClockConfig.class)
class ReservationExpireSchedulerTest extends BaseIntegrationTest {

    @Autowired private ReserveSeatUseCase reserveSeatUseCase;
    @Autowired private QueueService queueService;
    @Autowired private ReservationExpireScheduler scheduler;
    @Autowired private ReservationJpaRepository reservationJpaRepository;

    @Autowired private UserJpaRepository userRepo;
    @Autowired private ConcertJpaRepository concertRepo;
    @Autowired private ConcertDateJpaRepository dateRepo;
    @Autowired private ConcertSeatJpaRepository seatRepo;

    @Autowired private TestClocks.MutableClock clock;

    @Test
    @DisplayName("스케줄러가 만료된 PENDING 예약들을 EXPIRED로 일괄 변경한다")
    void scheduler_expires_staled_pending_reservations() {
        // 기본 데이터 생성
        var user = userRepo.save(UserEntity.builder()
                .userUuid("u-expire")
                .email("expire@test.com")
                .password("{noop}p")
                .name("expire-user")
                .build());

        var concert = concertRepo.save(ConcertEntity.builder()
                .title("title")
                .artistName("artist")
                .organizerName("org")
                .open(true)
                .build());

        LocalDate eventDate = LocalDate.now().plusDays(3);
        var date = dateRepo.save(ConcertDateEntity.builder()
                .concert(concert)
                .eventDate(eventDate)
                .venueName("venue")
                .open(true)
                .build());

        var seat = seatRepo.save(ConcertSeatEntity.builder()
                .concertDate(date)
                .seatNo(1)
                .section("A")
                .price(50_000L)
                .build());

        // 대기열 토큰 발급 + 좌석 홀드(holdSeconds = 5초)
        QueueEntry issue = queueService.issue(user.getUserUuid(), concert.getId());
        ReservationResponse first = reserveSeatUseCase.reserve(
                new ReserveSeatCommand(user.getUserUuid(), concert.getId(), eventDate, seat.getSeatNo(), 5)
        );

        // 사전 상태 확인 (PENDING + isActive = true)
        ReservationEntity before = reservationJpaRepository.findById(first.reservationId()).orElseThrow();
        assertThat(before.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(before.isActive()).isTrue();

        // 테스트용 시계 10초 전진 → holdExpiresAt 경과
        clock.advanceSeconds(10);

        // 스케줄러 직접 실행
        scheduler.expireStaledReservations();

        // 상태 확인
        ReservationEntity after = reservationJpaRepository.findById(first.reservationId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(after.isActive()).isFalse();
        assertThat(after.getExpiredAt()).isNotNull();
    }
}