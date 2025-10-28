package kr.hhplus.be.server.reservation.integration;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.reservation.SeatAlreadyReservedException;
import kr.hhplus.be.server.common.fixture.Concerts;
import kr.hhplus.be.server.common.fixture.Users;
import kr.hhplus.be.server.common.integration.BaseIntegrationTest;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertDateJpaRepository;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertJpaRepository;
import kr.hhplus.be.server.product.infrastructure.jpa.repository.ConcertSeatJpaRepository;
import kr.hhplus.be.server.reservation.application.port.in.command.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.port.in.usecase.ReserveSeatUseCase;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.repository.ReservationJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ReservationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    ReserveSeatUseCase reserveSeatUseCase;
    @Autowired
    ReservationJpaRepository reservationJpaRepository;

    // 픽스처 저장용 JPA 레포지토리
    @Autowired
    UserJpaRepository userRepo;
    @Autowired
    ConcertJpaRepository concertRepo;
    @Autowired
    ConcertDateJpaRepository dateRepo;
    @Autowired
    ConcertSeatJpaRepository seatRepo;

    private record Fx(String userUuid, Long concertId, LocalDate date, int seatNo) {
    }

    /**
     * 공용 픽스처로 실제 DB 레코드 한 세트 생성
     */
    private Fx prepare() {
        // 1) 사용자 (Users.entity() 사용)
        var userEntity = Users.entityWithUuid(UUID.randomUUID().toString());
        userEntity = userRepo.save(userEntity);

        // 2) 공연 (Concerts.entity() 사용)
        var concert = concertRepo.save(Concerts.entity());

        // 3) 공연 날짜 (Concerts.dateEntity(...))
        LocalDate eventDate = LocalDate.now().plusDays(7);
        var date = dateRepo.save(Concerts.dateEntity(concert, eventDate, true));

        // 4) 좌석 (Concerts.seatEntity(...))
        var seat = seatRepo.save(Concerts.seatEntity(date, 10, 50_000L));

        return new Fx(userEntity.getUserUuid(), concert.getId(), eventDate, seat.getSeatNo());
    }

    @Test
    @DisplayName("좌석 예약 성공 - 픽스처 사용")
    void reserve_seat_success() {
        // given
        Fx fx = prepare();

        // when
        var res = reserveSeatUseCase.reserve(
                new ReserveSeatCommand(fx.userUuid(), fx.concertId(), fx.date(), fx.seatNo(), 600)
        );

        // then
        assertThat(res).isNotNull();
        var entity = reservationJpaRepository.findById(res.reservationId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(entity.getHoldExpiresAt()).isAfter(entity.getCreatedAt());
    }

    @Test
    @DisplayName("예약된 좌석은 재예약 불가")
    void reserve_seat_conflict() {
        // given
        Fx fx = prepare();
        reserveSeatUseCase.reserve(
                new ReserveSeatCommand(fx.userUuid(), fx.concertId(), fx.date(), fx.seatNo(), 300)
        );

        // when & then
        assertThatThrownBy(() ->
                reserveSeatUseCase.reserve(
                        new ReserveSeatCommand(fx.userUuid(), fx.concertId(), fx.date(), fx.seatNo(), 300)
                )
        ).isInstanceOf(SeatAlreadyReservedException.class);
    }
}