package kr.hhplus.be.server.reservation.application.usecase;

import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.ReservationNotFoundException;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.reservation.application.port.in.result.ReservationDetailResponse;
import kr.hhplus.be.server.reservation.application.port.out.ReservationRepository;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetReservationServiceTest {

    private ReservationRepository reservationRepository;
    private UserJpaRepository userRepo;
    private GetReservationService sut;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        userRepo = mock(UserJpaRepository.class);
        sut = new GetReservationService(reservationRepository, userRepo);
    }

    @Test
    void get_success() {
        String userUuid = "u-uuid";
        Long userId = 10L;
        Long resId = 777L;

        when(userRepo.findIdByUserUuid(userUuid)).thenReturn(java.util.Optional.of(userId));

        var found = new Reservation(
                resId, userId, 101L, 1001L, 5001L,
                ReservationStatus.PENDING, 30000L,
                LocalDateTime.now().plusMinutes(5),
                null, null, null,
                null,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().minusMinutes(1)
        );
        when(reservationRepository.findById(resId)).thenReturn(java.util.Optional.of(found));

        ReservationDetailResponse dto = sut.get(userUuid, resId);

        assertThat(dto.reservationId()).isEqualTo(resId);
        assertThat(dto.concertId()).isEqualTo(101L);
        assertThat(dto.status()).isEqualTo("PENDING");
        assertThat(dto.paidAmount()).isEqualTo(30000L);
    }

    @Test
    void get_fail_when_user_not_found() {
        when(userRepo.findIdByUserUuid("missing")).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> sut.get("missing", 1L))
                .isInstanceOf(ReservationAccessDeniedException.class);
    }

    @Test
    void get_fail_when_reservation_not_found() {
        when(userRepo.findIdByUserUuid("u")).thenReturn(java.util.Optional.of(1L));
        when(reservationRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> sut.get("u", 999L))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void getMy_success() {
        when(userRepo.findIdByUserUuid("u")).thenReturn(java.util.Optional.of(1L));
        var r1 = new Reservation(1L, 1L, 10L, 100L, 1000L, ReservationStatus.PENDING, 1000L, null, null, null, null, null, LocalDateTime.now(), LocalDateTime.now());
        var r2 = new Reservation(2L, 1L, 11L, 101L, 1001L, ReservationStatus.CANCELED, 2000L, null, null, LocalDateTime.now(), null, null, LocalDateTime.now(), LocalDateTime.now());
        when(reservationRepository.findByUserId(1L)).thenReturn(List.of(r1, r2));

        var list = sut.getMy("u");

        assertThat(list).hasSize(2);
        assertThat(list.get(0).reservationId()).isEqualTo(1L);
        assertThat(list.get(0).status()).isEqualTo("PENDING");
        assertThat(list.get(1).status()).isEqualTo("CANCELED");
    }
}