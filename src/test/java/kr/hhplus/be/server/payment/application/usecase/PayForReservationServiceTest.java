package kr.hhplus.be.server.payment.application.usecase;

import kr.hhplus.be.server.payment.application.port.in.command.PayForReservationCommand;
import kr.hhplus.be.server.payment.application.port.in.result.PaymentResponse;
import kr.hhplus.be.server.payment.application.port.out.*;
import kr.hhplus.be.server.payment.domain.model.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * PayForReservationService 단위 테스트
 */
class PayForReservationServiceTest {

    private QueuePort queuePort;
    private ReservationPort reservationPort;
    private WalletPort walletPort;
    private PaymentRepository paymentRepository;

    private PayForReservationService sut;

    @BeforeEach
    void setUp() {
        queuePort = mock(QueuePort.class);
        reservationPort = mock(ReservationPort.class);
        walletPort = mock(WalletPort.class);
        paymentRepository = mock(PaymentRepository.class);

        sut = new PayForReservationService(queuePort, reservationPort, walletPort, paymentRepository);
    }

    @Test
    void pay_success_flow() {
        // given
        String userUuid = "u-uuid";
        Long reservationId = 100L;
        Long userId = 10L;
        Long concertId = 77L;
        long amount = 55_000L;
        String idem = "idem-123";

        // 1) 대기열 검증 OK (아무 것도 안 던짐)
        doNothing().when(queuePort).validate(userUuid, "queue-token");

        // 2) 예약 로딩/검증 OK
        ReservationForPayment res = new ReservationForPayment(reservationId, userId, concertId, amount);
        when(reservationPort.getReservationForPayment(reservationId, userUuid)).thenReturn(res);

        // 3) 멱등키 미사용 기록
        when(paymentRepository.existsByIdempotencyKey(idem)).thenReturn(false);

        // 4) 지갑 차감 OK
        doNothing().when(walletPort).debit(userId, amount, null, idem);

        // 5) 결제 저장 OK -> 저장된 Payment의 id가 필요하므로 mock Payment 사용
        Payment savedPayment = mock(Payment.class);
        when(savedPayment.getId()).thenReturn(999L);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // 6) 예약 확정 OK
        doNothing().when(reservationPort).confirm(reservationId, userUuid);

        // 7) 대기열 만료 OK
        doNothing().when(queuePort).expireToken(userUuid, concertId);

        PayForReservationCommand cmd = new PayForReservationCommand(
                userUuid, reservationId, amount, "queue-token", idem
        );

        // when
        PaymentResponse resp = sut.pay(cmd);

        // then
        assertThat(resp.confirmed()).isTrue();
        assertThat(resp.reservationId()).isEqualTo(reservationId);
        assertThat(resp.paymentId()).isEqualTo(999L);

        // 호출 검증
        verify(queuePort).validate(userUuid, "queue-token");
        verify(reservationPort).getReservationForPayment(reservationId, userUuid);
        verify(paymentRepository).existsByIdempotencyKey(idem);
        verify(walletPort).debit(userId, amount, null, idem);
        verify(paymentRepository).save(any(Payment.class));
        verify(reservationPort).confirm(reservationId, userUuid);
        verify(queuePort).expireToken(userUuid, concertId);
        verifyNoMoreInteractions(queuePort, reservationPort, walletPort, paymentRepository);
    }

    @Test
    void pay_idempotent_repeat_returns_success_without_side_effects() {
        // given
        String userUuid = "u-uuid";
        Long reservationId = 200L;
        Long userId = 20L;
        Long concertId = 88L;
        long amount = 66_000L;
        String idem = "idem-dup";

        // validate OK
        doNothing().when(queuePort).validate(userUuid, "queue-token");

        // 예약 로딩/검증 OK
        ReservationForPayment res = new ReservationForPayment(reservationId, userId, concertId, amount);
        when(reservationPort.getReservationForPayment(reservationId, userUuid)).thenReturn(res);

        // 멱등키 이미 존재
        when(paymentRepository.existsByIdempotencyKey(idem)).thenReturn(true);

        PayForReservationCommand cmd = new PayForReservationCommand(
                userUuid, reservationId, amount, "queue-token", idem
        );

        // when
        PaymentResponse resp = sut.pay(cmd);

        // then
        assertThat(resp.confirmed()).isTrue();
        assertThat(resp.reservationId()).isEqualTo(reservationId);
        assertThat(resp.paymentId()).isNull(); // 저장/지갑/확정/만료 등 부수효과 없이 반환

        // 지갑/저장/확정/만료는 호출 안됨
        verify(queuePort).validate(userUuid, "queue-token");
        verify(reservationPort).getReservationForPayment(reservationId, userUuid);
        verify(paymentRepository).existsByIdempotencyKey(idem);

        verifyNoInteractions(walletPort);
        verify(paymentRepository, never()).save(any());
        verify(reservationPort, never()).confirm(anyLong(), anyString());
        verify(queuePort, never()).expireToken(anyString(), anyLong());
        verifyNoMoreInteractions(queuePort, reservationPort, paymentRepository);
    }

    @Test
    void pay_fails_when_queue_validation_fails() {
        // given
        String userUuid = "u-uuid";
        PayForReservationCommand cmd = new PayForReservationCommand(
                userUuid, 1L, 1000L, "bad-queue-token", "idem-x"
        );

        // validate 에서 예외 발생 시나리오
        doThrow(new RuntimeException("invalid queue"))
                .when(queuePort).validate(userUuid, "bad-queue-token");

        // expect
        assertThatThrownBy(() -> sut.pay(cmd))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("invalid queue");

        // 이후 단계는 전혀 호출되지 않아야 함
        verify(queuePort).validate(userUuid, "bad-queue-token");
        verifyNoInteractions(reservationPort, walletPort, paymentRepository);
    }
}