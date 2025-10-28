package kr.hhplus.be.server.reservation.integration;

import kr.hhplus.be.server.common.integration.BaseIntegrationTest;
import kr.hhplus.be.server.reservation.application.port.in.command.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.port.in.result.ReservationResponse;
import kr.hhplus.be.server.reservation.application.port.in.usecase.ReserveSeatUseCase;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.repository.ReservationJpaRepository;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;
import kr.hhplus.be.server.common.enums.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 통합 테스트: 실제 DB(MySQL Testcontainers) + JPA + Adapter 계층을 모두 포함
 */
class ReservationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ReserveSeatUseCase reserveSeatUseCase;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Test
    @DisplayName("좌석 예약 성공 - 실제 DB 통합 테스트")
    void reserve_seat_success() {
        // given
        String userUuid = "user-uuid-1";
        Long concertId = 1L;
        LocalDate date = LocalDate.of(2025, 11, 1);
        int seatNo = 10;
        long holdSeconds = 600L;

        // when
        ReservationResponse response =
                reserveSeatUseCase.reserve(new ReserveSeatCommand(userUuid, concertId, date, seatNo, holdSeconds));

        // then
        assertThat(response).isNotNull();
        assertThat(response.reservationId()).isNotNull();

        // DB 상태 검증
        ReservationEntity entity = reservationJpaRepository.findById(response.reservationId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(entity.getHoldExpiresAt()).isAfter(entity.getCreatedAt());
    }

    @Test
    @DisplayName("예약된 좌석은 재예약 불가해야 한다")
    void reserve_seat_conflict() {
        // given: 이미 예약된 좌석 하나
        String userUuid = "user-uuid-1";
        Long concertId = 1L;
        LocalDate date = LocalDate.of(2025, 11, 1);
        int seatNo = 15;
        long holdSeconds = 300L;

        // 첫 번째 예약 성공
        reserveSeatUseCase.reserve(new ReserveSeatCommand(userUuid, concertId, date, seatNo, holdSeconds));

        // when
        try {
            reserveSeatUseCase.reserve(new ReserveSeatCommand(userUuid, concertId, date, seatNo, holdSeconds));
        } catch (Exception e) {
            // then
            assertThat(e.getClass().getSimpleName()).isEqualTo("SeatAlreadyReservedException");
        }
    }
}