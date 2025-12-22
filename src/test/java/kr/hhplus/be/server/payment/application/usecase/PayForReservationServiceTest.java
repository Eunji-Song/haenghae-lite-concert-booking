package kr.hhplus.be.server.payment.application.usecase;

import kr.hhplus.be.server.payment.application.event.PaymentCompletedEvent;
import kr.hhplus.be.server.payment.application.port.in.command.PayForReservationCommand;
import kr.hhplus.be.server.payment.application.port.in.result.PaymentResponse;
import kr.hhplus.be.server.payment.application.port.out.*;
import kr.hhplus.be.server.payment.domain.model.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PayForReservationService 단위 테스트
 *
 * - 결제 트랜잭션의 "핵심 흐름"을 단위 수준에서 검증한다.
 * - 결제 완료 이벤트는 publishEvent 호출 여부로만 검증한다. (리스너 동작은 별도 테스트 범위)
 */
class PayForReservationServiceTest {

    private QueuePort queuePort;
    private ReservationPort reservationPort;
    private WalletPort walletPort;
    private PaymentRepository paymentRepository;

    private ApplicationEventPublisher eventPublisher;
    private Clock clock;

    private PayForReservationService sut;

    @BeforeEach
    void setUp() {
        queuePort = mock(QueuePort.class);
        reservationPort = mock(ReservationPort.class);
        walletPort = mock(WalletPort.class);
        paymentRepository = mock(PaymentRepository.class);

        eventPublisher = mock(ApplicationEventPublisher.class);

        // 이벤트 occurredAt을 예측 가능하게 만들기 위해 고정 시계 사용
        clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

        sut = new PayForReservationService(
                queuePort,
                reservationPort,
                walletPort,
                paymentRepository,
                eventPublisher,
                clock
        );
    }

    @Test
    void pay_success_flow_publishes_event() {
        // given
        String userUuid = "u-uuid";
        Long reservationId = 100L;
        Long userId = 10L;
        Long concertId = 77L;
        long amount = 55_000L;
        String idem = "idem-123";
        String queueToken = "queue-token";

        // 1) 대기열 검증 OK
        doNothing().when(queuePort).validate(userUuid, queueToken);

        // 2) 예약 로딩/검증 OK
        ReservationForPayment res = new ReservationForPayment(reservationId, userId, concertId, amount);
        when(reservationPort.getReservationForPayment(reservationId, userUuid)).thenReturn(res);

        // 3) 멱등키 없음
        when(paymentRepository.existsByIdempotencyKey(idem)).thenReturn(false);

        // 4) 지갑 차감 OK
        doNothing().when(walletPort).debit(userId, amount, null, idem);

        // 5) 결제 저장 OK
        Payment savedPayment = mock(Payment.class);
        when(savedPayment.getId()).thenReturn(999L);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // 6) 예약 확정 OK
        doNothing().when(reservationPort).confirm(reservationId, userUuid);

        // 7) 대기열 만료 OK
        doNothing().when(queuePort).expireToken(userUuid, concertId);

        PayForReservationCommand cmd = new PayForReservationCommand(
                userUuid, reservationId, amount, queueToken, idem
        );

        // when
        PaymentResponse resp = sut.pay(cmd);

        // then
        assertThat(resp.confirmed()).isTrue();
        assertThat(resp.reservationId()).isEqualTo(reservationId);
        assertThat(resp.paymentId()).isEqualTo(999L);

        // 호출 검증
        verify(queuePort).validate(userUuid, queueToken);
        verify(reservationPort).getReservationForPayment(reservationId, userUuid);
        verify(paymentRepository).existsByIdempotencyKey(idem);
        verify(walletPort).debit(userId, amount, null, idem);
        verify(paymentRepository).save(any(Payment.class));
        verify(reservationPort).confirm(reservationId, userUuid);
        verify(queuePort).expireToken(userUuid, concertId);

        // 이벤트 발행 검증
        verify(eventPublisher).publishEvent(any(PaymentCompletedEvent.class));

        verifyNoMoreInteractions(queuePort, reservationPort, walletPort, paymentRepository, eventPublisher);
    }

    @Test
    void pay_idempotent_repeat_returns_success_without_side_effects_and_no_event() {
        // given
        String userUuid = "u-uuid";
        Long reservationId = 200L;
        Long userId = 20L;
        Long concertId = 88L;
        long amount = 66_000L;
        String idem = "idem-dup";
        String queueToken = "queue-token";

        // validate OK
        doNothing().when(queuePort).validate(userUuid, queueToken);

        // 예약 로딩/검증 OK
        ReservationForPayment res = new ReservationForPayment(reservationId, userId, concertId, amount);
        when(reservationPort.getReservationForPayment(reservationId, userUuid)).thenReturn(res);

        // 멱등키 이미 존재
        when(paymentRepository.existsByIdempotencyKey(idem)).thenReturn(true);

        PayForReservationCommand cmd = new PayForReservationCommand(
                userUuid, reservationId, amount, queueToken, idem
        );

        // when
        PaymentResponse resp = sut.pay(cmd);

        // then
        assertThat(resp.confirmed()).isTrue();
        assertThat(resp.reservationId()).isEqualTo(reservationId);
        assertThat(resp.paymentId()).isNull();

        // 멱등 처리 시 호출되는 범위만 검증
        verify(queuePort).validate(userUuid, queueToken);
        verify(reservationPort).getReservationForPayment(reservationId, userUuid);
        verify(paymentRepository).existsByIdempotencyKey(idem);

        // 부수 효과는 없어야 함
        verifyNoInteractions(walletPort);
        verify(paymentRepository, never()).save(any());
        verify(reservationPort, never()).confirm(anyLong(), anyString());
        verify(queuePort, never()).expireToken(anyString(), anyLong());

        // 이벤트도 재발행하지 않음
        verify(eventPublisher, never()).publishEvent(any());

        verifyNoMoreInteractions(queuePort, reservationPort, paymentRepository, eventPublisher);
    }

    @Test
    void pay_fails_when_queue_validation_fails_and_no_event() {
        // given
        String userUuid = "u-uuid";
        String queueToken = "bad-queue-token";

        PayForReservationCommand cmd = new PayForReservationCommand(
                userUuid, 1L, 1000L, queueToken, "idem-x"
        );

        doThrow(new RuntimeException("invalid queue"))
                .when(queuePort).validate(userUuid, queueToken);

        // expect
        assertThatThrownBy(() -> sut.pay(cmd))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("invalid queue");

        // then: 이후 단계는 전혀 호출되지 않아야 함
        verify(queuePort).validate(userUuid, queueToken);

        verifyNoInteractions(reservationPort, walletPort, paymentRepository, eventPublisher);
        verifyNoMoreInteractions(queuePort);
    }
}