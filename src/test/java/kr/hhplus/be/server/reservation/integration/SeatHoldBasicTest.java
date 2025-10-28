package kr.hhplus.be.server.reservation.integration;

import kr.hhplus.be.server.common.integration.BaseIntegrationTest;
import kr.hhplus.be.server.common.exception.reservation.SeatAlreadyReservedException;
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
import kr.hhplus.be.server.reservation.application.port.in.usecase.ReserveSeatUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SeatHoldBasicTest extends BaseIntegrationTest {

    @Autowired
    ReserveSeatUseCase reserveSeatUseCase;
    @Autowired
    QueueService queueService;
    @Autowired
    UserJpaRepository userRepo;
    @Autowired
    ConcertJpaRepository concertRepo;
    @Autowired
    ConcertDateJpaRepository dateRepo;
    @Autowired
    ConcertSeatJpaRepository seatRepo;

    @Test
    @DisplayName("먼저 홀드된 좌석은 이후 동일 좌석 요청을 거부한다")
    void seatHold_shouldFailIfSeatAlreadyHeld() {
        var user = userRepo.save(UserEntity.builder()
                .userUuid("u1").email("u1@test.com").password("{noop}p").name("u1").build());
        var concert = concertRepo.save(ConcertEntity.builder()
                .title("t").artistName("a").organizerName("o").open(true).build());
        var eventDate = LocalDate.now().plusDays(3);
        var date = dateRepo.save(ConcertDateEntity.builder()
                .concert(concert).eventDate(eventDate).venueName("v").open(true).build());
        var seat = seatRepo.save(ConcertSeatEntity.builder()
                .concertDate(date).seatNo(1).section("A").price(50_000L).build());

        queueService.issue(user.getUserUuid(), concert.getId());

        // 예약
        var res1 = reserveSeatUseCase.reserve(
                new ReserveSeatCommand(user.getUserUuid(), concert.getId(), eventDate, seat.getSeatNo(), 300));
        assertThat(res1.reservationId()).isNotNull();

        // 같은 좌석 재요청 → 거부
        assertThatThrownBy(() -> reserveSeatUseCase.reserve(
                new ReserveSeatCommand(user.getUserUuid(), concert.getId(), eventDate, seat.getSeatNo(), 300)
        )).isInstanceOf(SeatAlreadyReservedException.class);
    }
}