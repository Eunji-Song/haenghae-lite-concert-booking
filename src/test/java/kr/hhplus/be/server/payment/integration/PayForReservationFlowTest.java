package kr.hhplus.be.server.payment.integration;


import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.common.enums.ReservationStatus;
import kr.hhplus.be.server.common.integration.BaseIntegrationTest;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.payment.application.port.in.command.PayForReservationCommand;
import kr.hhplus.be.server.payment.application.port.in.result.PaymentResponse;
import kr.hhplus.be.server.payment.application.port.in.usecase.PayForReservationUseCase;
import kr.hhplus.be.server.payment.application.port.out.PaymentRepository;
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
import kr.hhplus.be.server.wallet.domain.model.WalletAccount;
import kr.hhplus.be.server.wallet.domain.repository.WalletAccountRepository;
import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.WalletAccountEntity;
import kr.hhplus.be.server.wallet.infrastructure.jpa.repository.WalletAccountJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PayForReservationFlowTest extends BaseIntegrationTest {

    @Autowired
    PayForReservationUseCase payForReservationUseCase;
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

    @Autowired
    WalletAccountRepository walletAccountRepository;
    @Autowired
    WalletAccountJpaRepository walletAccountJpaRepository;

    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    ReservationJpaRepository reservationJpaRepository;

    @Test
    @DisplayName("대기열 토큰 + 좌석 예약 + 결제까지 전체 플로우가 성공적으로 동작한다")
    void pay_flow_success() {
        // 유저 생성
        String userUuid = UUID.randomUUID().toString();
        UserEntity user = userRepo.save(
                UserEntity.builder()
                        .userUuid(userUuid)
                        .email("payflow@test.com")
                        .password("{noop}p")
                        .name("pay-user")
                        .build()
        );
        Long userId = user.getId();

        // 콘서트 + 날짜 + 좌석 생성
        ConcertEntity concert = concertRepo.save(
                ConcertEntity.builder()
                        .title("테스트 콘서트")
                        .artistName("테스트 가수")
                        .organizerName("테스트 주최")
                        .open(true)
                        .build()
        );

        LocalDate eventDate = LocalDate.now().plusDays(7);
        ConcertDateEntity date = dateRepo.save(
                ConcertDateEntity.builder()
                        .concert(concert)
                        .eventDate(eventDate)
                        .venueName("올림픽홀")
                        .open(true)
                        .build()
        );

        long seatPrice = 50000L;
        ConcertSeatEntity seat = seatRepo.save(
                ConcertSeatEntity.builder()
                        .concertDate(date)
                        .seatNo(1)
                        .section("A")
                        .price(seatPrice)
                        .build()
        );

        // 지갑 계좌 생성 + 잔액 충전 (예: 100,000원)
        WalletAccount account = walletAccountRepository.createIfNotExists(userId);
        account.charge(100000L);
        walletAccountRepository.save(account);


        long initialBalance = account.getBalance();

        // 대기열 토큰 발급
        QueueEntry entry = queueService.issue(userUuid, concert.getId());

        // 좌석 예약(PENDING)
        ReservationResponse reservationRes = reserveSeatUseCase.reserve(
                new ReserveSeatCommand(
                        userUuid,
                        concert.getId(),
                        eventDate,
                        seat.getSeatNo(),
                        300
                )
        );

        Long reservationId = reservationRes.reservationId();
        assertThat(reservationId).isNotNull();

        // 사전 상태 확인: 예약 PENDING
        ReservationEntity beforePayment = reservationJpaRepository.findById(reservationId).orElseThrow();
        assertThat(beforePayment.getStatus()).isEqualTo(ReservationStatus.PENDING);

        // 결제 요청
        String idemKey = "pay-flow-" + reservationId;
        PayForReservationCommand cmd = new PayForReservationCommand(
                userUuid,
                reservationId,
                null,
                entry.token(),
                idemKey
        );

        PaymentResponse payRes = payForReservationUseCase.pay(cmd);

        // 결제 결과 검증

        // PaymentResponse 기본 검증
        assertThat(payRes.confirmed()).isTrue();
        assertThat(payRes.reservationId()).isEqualTo(reservationId);
        assertThat(payRes.paymentId()).isNotNull();

        // 결제 저장 여부 검증 (도메인 PaymentRepository 기준)
        var paymentOpt = paymentRepository.findSucceededByReservationId(reservationId);
        assertThat(paymentOpt).isPresent();

        var payment = paymentOpt.get();
        assertThat(payment.getReservationId()).isEqualTo(reservationId);
        assertThat(payment.getAmount()).isEqualTo(seatPrice);

        // 예약 상태 CONFIRMED 로 변경되었는지
        ReservationEntity afterPayment = reservationJpaRepository.findById(reservationId).orElseThrow();
        assertThat(afterPayment.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        // 지갑 잔액이 seatPrice 만큼 차감되었는지
        WalletAccountEntity walletAfter = walletAccountJpaRepository.findByUserId(userId)
                .orElseThrow();
        assertThat(walletAfter.getBalance())
                .as("지갑 잔액은 초기 잔액 - 결제 금액이어야 함")
                .isEqualTo(initialBalance - seatPrice);

        // 대기열 토큰이 EXPIRED 되면 조회 불가능해야 한다 (삭제 정책)
        assertThat(queueService.findByToken(entry.token()))
                .as("결제 완료 후 대기열 토큰은 만료 정책에 따라 제거된다")
                .isEmpty();

    }
}