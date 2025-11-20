package kr.hhplus.be.server.booking.integration;

import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.integration.BaseIntegrationTest;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.payment.application.port.in.command.PayForReservationCommand;
import kr.hhplus.be.server.payment.application.port.in.result.PaymentResponse;
import kr.hhplus.be.server.payment.application.port.in.usecase.PayForReservationUseCase;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.repository.PaymentJpaRepository;
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
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.repository.ReservationJpaRepository;
import kr.hhplus.be.server.wallet.application.service.WalletService;
import kr.hhplus.be.server.wallet.infrastructure.jpa.repository.WalletAccountJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 엔드투엔드 통합 플로우:
 * 1) 유저/공연/일자/좌석 준비
 * 2) 대기열 토큰 발급
 * 3) 좌석 예약(PENDING, hold)
 * 4) 지갑 충전(멱등)
 * 5) 결제 요청 → 예약 확정, 결제 저장, 대기열 만료
 *
 * Testcontainers MySQL + 실제 JPA/Adapter/Service 전체 연동
 */
class BookingFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired private UserJpaRepository userRepo;
    @Autowired private WalletService walletService;

    @Autowired private ConcertJpaRepository concertRepo;
    @Autowired private ConcertDateJpaRepository dateRepo;
    @Autowired private ConcertSeatJpaRepository seatRepo;

    @Autowired private QueueService queueService;

    @Autowired private ReserveSeatUseCase reserveSeatUseCase;
    @Autowired private ReservationJpaRepository reservationJpaRepository;

    @Autowired private PayForReservationUseCase payForReservationUseCase;
    @Autowired private PaymentJpaRepository paymentJpaRepository;

    @Autowired private WalletAccountJpaRepository walletAccountJpaRepository;

    @Test
    @DisplayName("유저가 토큰 발급 → 좌석 예약 → 지갑 충전 → 결제 성공 → 예약 확정 & 토큰 만료")
    void booking_flow_success() {
        // ---------------------------------------------------------------------
        // 0) 테스트 데이터 준비
        // ---------------------------------------------------------------------
        // 유저
        UserEntity user = userRepo.save(UserEntity.builder()
                .userUuid("user-uuid-100")
                .email("flow@test.com")
                .password("{noop}hash")
                .name("FlowUser")
                .build());

        // 공연/일자/좌석
        ConcertEntity concert = concertRepo.save(ConcertEntity.builder()
                .title("Flow Concert")
                .description("Flow 통합 테스트 콘서트")
                .artistName("Flow Artist")
                .organizerName("Flow Org")
                .open(true)
                .build());

        var eventDate = LocalDate.of(2025, 11, 1);
        ConcertDateEntity date = dateRepo.save(ConcertDateEntity.builder()
                .concert(concert)
                .eventDate(eventDate)
                .venueName("Flow Hall")
                .open(true)
                .build());

        long seatPrice = 50_000L;
        ConcertSeatEntity seat = seatRepo.save(ConcertSeatEntity.builder()
                .concertDate(date)
                .seatNo(17)
                .section("A")
                .price(seatPrice)
                .build());

        // 지갑 계정(없으면 생성됨) + 잔액 충전(멱등 키)
        String idemKey = UUID.randomUUID().toString();
        walletService.charge(user.getUserUuid(), 100_000L, idemKey);
        assertThat(walletAccountJpaRepository.findById(user.getId()))
                .isPresent()
                .get()
                .extracting("balance")
                .isEqualTo(100_000L);

        // ---------------------------------------------------------------------
        // 1) 대기열 토큰 발급
        // ---------------------------------------------------------------------
        QueueEntry entry = queueService.issue(user.getUserUuid(), concert.getId());
        assertThat(entry).isNotNull();
        assertThat(entry.token()).isNotBlank();
        assertThat(entry.status()).isIn(QueueStatus.ISSUED, QueueStatus.ACTIVE);

        // ---------------------------------------------------------------------
        // 2) 좌석 예약 (PENDING + 홀드)
        // ---------------------------------------------------------------------
        ReservationResponse reserveRes = reserveSeatUseCase.reserve(
                new ReserveSeatCommand(
                        user.getUserUuid(),
                        concert.getId(),
                        eventDate,
                        seat.getSeatNo(),
                        600
                )
        );
        assertThat(reserveRes.reservationId()).isNotNull();

        ReservationEntity pending = reservationJpaRepository.findById(reserveRes.reservationId()).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(ReservationStatus.PENDING);

        // ---------------------------------------------------------------------
        // 3) 결제 (멱등 키 미지정 → Payment 서비스에서 생성 or 지정 가능)
        //    주의: 현재 ReserveSeatService가 amount=0L로 저장하는 설계라면,
        //          PaymentUseCase에 전달하는 amount도 0L로 맞춘다(검증 로직 일치용).
        //          좌석 가격 기반 검증으로 개선 시 seatPrice를 넣으면 된다.
        // ---------------------------------------------------------------------
        PaymentResponse payRes = payForReservationUseCase.pay(
                new PayForReservationCommand(
                        user.getUserUuid(),
                        reserveRes.reservationId(),
                        0L,
                        entry.token(),
                        "idem.pay.flow.1"
                )
        );


        assertThat(payRes).isNotNull();
        assertThat(payRes.confirmed()).isTrue();
        assertThat(payRes.paymentId()).isNotNull();

        // 결제 저장 확인
        assertThat(paymentJpaRepository.findById(payRes.paymentId())).isPresent();

        // 예약 확정 확인
        ReservationEntity confirmed = reservationJpaRepository.findById(reserveRes.reservationId()).orElseThrow();
        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(confirmed.getConfirmedAt()).isNotNull();

        // 대기열 토큰 만료(삭제) 확인
        assertThat(queueService.findByToken(entry.token())).isEmpty();

        // 지갑 잔액 검증 (현재 amount=0L 결제면 변화 없음 / 좌석가격 검증으로 바꾸면 50,000 차감 기대)
        assertThat(walletAccountJpaRepository.findById(user.getId()))
                .isPresent()
                .get()
                .extracting("balance")
                .isEqualTo(50_000L);
    }
}