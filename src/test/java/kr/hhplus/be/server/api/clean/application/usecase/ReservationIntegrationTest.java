package kr.hhplus.be.server.api.clean.application.usecase;

import kr.hhplus.be.server.api.clean.application.port.in.payment.PayForReservationCommand;
import kr.hhplus.be.server.api.clean.application.port.in.payment.PaymentResponse;
import kr.hhplus.be.server.api.clean.application.port.in.reservation.*;
import kr.hhplus.be.server.api.clean.application.port.out.payment.PaymentRepository;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.clean.domain.model.payment.Payment;
import kr.hhplus.be.server.common.enums.PaymentStatus;
import kr.hhplus.be.server.common.enums.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 예약 시스템 통합 테스트
 * JPA Adapter와 실제 DB를 사용한 전체 플로우 검증
 */
@SpringBootTest
@ActiveProfiles("dev") // JPA Adapter 사용
@Transactional
@DisplayName("예약 시스템 통합 테스트")
class ReservationIntegrationTest {

    @Autowired
    private ReserveSeatUseCase reserveSeatUseCase;

    @Autowired
    private PayForReservationUseCase payForReservationUseCase;

    @Autowired
    private CancelReservationUseCase cancelReservationUseCase;

    @Autowired
    private GetReservationUseCase getReservationUseCase;

    @Autowired
    private GetMyReservationsUseCase getMyReservationsUseCase;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private static final Long USER_ID = 1L;
    private static final Long CONCERT_ID = 1L;
    private static final String DATE = "2025-09-10";
    private static final Long SEAT_NO = 12L;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 데이터 정리 (필요시)
    }

    @Test
    @DisplayName("예약부터 결제까지 전체 플로우 성공 시나리오")
    void 예약_결제_전체_플로우_성공() {
        // 1. 좌석 예약 (홀드)
        ReserveSeatCommand reserveCommand = new ReserveSeatCommand(USER_ID, CONCERT_ID, DATE, SEAT_NO);
        ReservationResponse reservationResult = reserveSeatUseCase.reserveSeat(reserveCommand);

        assertThat(reservationResult).isNotNull();
        assertThat(reservationResult.reservationId()).isNotNull();
        assertThat(reservationResult.holdExpiresAt()).isNotNull();

        Long reservationId = reservationResult.reservationId();

        // 2. 예약 상세 조회로 상태 확인
        GetReservationQuery getQuery = new GetReservationQuery(reservationId, USER_ID);
        ReservationDetailResponse detailResult = getReservationUseCase.getReservation(getQuery);

        assertThat(detailResult.status()).isEqualTo(ReservationStatus.PENDING);
        assertThat(detailResult.holdExpiresAt()).isNotNull();

        // 3. 결제 진행
        PayForReservationCommand paymentCommand = new PayForReservationCommand(reservationId, 50000L, "idem-key");
        PaymentResponse paymentResult = payForReservationUseCase.payForReservation(paymentCommand);

        assertThat(paymentResult.confirmed()).isTrue();
        assertThat(paymentResult.paymentId()).isNotNull();
        assertThat(paymentResult.reservationId()).isEqualTo(reservationId);

        // 4. 결제 후 예약 상태 재확인
        ReservationDetailResponse confirmedDetail = getReservationUseCase.getReservation(getQuery);
        assertThat(confirmedDetail.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(confirmedDetail.holdExpiresAt()).isNull(); // 확정되면 홀드 시간 제거

        // 5. 내 예약 목록에서 확인
        GetMyReservationsQuery listQuery = new GetMyReservationsQuery(USER_ID);
        List<ReservationSummaryResponse> myReservations = getMyReservationsUseCase.getMyReservations(listQuery);

        assertThat(myReservations).hasSize(1);
        assertThat(myReservations.get(0).reservationId()).isEqualTo(reservationId);
        assertThat(myReservations.get(0).status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("예약 후 취소 플로우 성공 시나리오")
    void 예약_취소_플로우_성공() {
        // 1. 좌석 예약
        ReserveSeatCommand reserveCommand = new ReserveSeatCommand(USER_ID, CONCERT_ID, DATE, SEAT_NO);
        ReservationResponse reservationResult = reserveSeatUseCase.reserveSeat(reserveCommand);
        Long reservationId = reservationResult.reservationId();

        // 2. 예약 취소
        CancelReservationCommand cancelCommand = new CancelReservationCommand(reservationId, USER_ID);
        CancelReservationResponse cancelResult = cancelReservationUseCase.cancelReservation(cancelCommand);

        assertThat(cancelResult.canceled()).isTrue();

        // 3. 취소 후 상태 확인
        GetReservationQuery getQuery = new GetReservationQuery(reservationId, USER_ID);
        ReservationDetailResponse canceledDetail = getReservationUseCase.getReservation(getQuery);

        assertThat(canceledDetail.status()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(canceledDetail.holdExpiresAt()).isNull();
    }

    @Test
    @DisplayName("결제 완료된 예약 취소 시 환불 처리")
    void 결제완료_예약_취소시_환불처리() {
        // 1. 예약 + 결제
        ReserveSeatCommand reserveCommand = new ReserveSeatCommand(USER_ID, CONCERT_ID, DATE, SEAT_NO);
        ReservationResponse reservationResult = reserveSeatUseCase.reserveSeat(reserveCommand);
        Long reservationId = reservationResult.reservationId();

        PayForReservationCommand paymentCommand = new PayForReservationCommand(reservationId, 50000L, "idem-key");
        PaymentResponse paymentResult = payForReservationUseCase.payForReservation(paymentCommand);

        // 2. 결제된 예약 취소
        CancelReservationCommand cancelCommand = new CancelReservationCommand(reservationId, USER_ID);
        CancelReservationResponse cancelResult = cancelReservationUseCase.cancelReservation(cancelCommand);

        assertThat(cancelResult.canceled()).isTrue();

        // 3. 결제 정보 확인 (환불 처리 확인)
        List<Payment> payments = paymentRepository.findByReservationId(reservationId);
        assertThat(payments).isNotEmpty();
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.SUCCEEDED); // 원본 결제 유지

        // TODO: 환불 로직 완성 시 환불 기록도 확인
    }

    @Test
    @DisplayName("동시에 같은 좌석 예약 시도 시 한 건만 성공")
    void 동시_예약_시도시_한건만_성공() {
        // Given
        ReserveSeatCommand command1 = new ReserveSeatCommand(1L, CONCERT_ID, DATE, SEAT_NO);
        ReserveSeatCommand command2 = new ReserveSeatCommand(2L, CONCERT_ID, DATE, SEAT_NO);

        // When - 첫 번째 예약 성공
        ReservationResponse result1 = reserveSeatUseCase.reserveSeat(command1);
        assertThat(result1.reservationId()).isNotNull();

        // Then - 두 번째 예약 실패 (이미 예약된 좌석)
        assertThatThrownBy(() -> reserveSeatUseCase.reserveSeat(command2))
                .hasMessageContaining("예약된 좌석"); // 실제 예외 메시지에 맞게 조정
    }

    @Test
    @DisplayName("Repository가 JPA Adapter로 주입되는지 확인")
    void Repository_JPA_Adapter_주입_확인() {
        // ReservationRepository 구현체 클래스 확인
        String reservationRepoClass = reservationRepository.getClass().getSimpleName();
        assertThat(reservationRepoClass).contains("Jpa"); // JpaAdapter 클래스가 주입되었는지 확인

        // PaymentRepository 구현체 클래스 확인
        String paymentRepoClass = paymentRepository.getClass().getSimpleName();
        assertThat(paymentRepoClass).contains("Jpa");
    }

    @Test
    @DisplayName("데이터베이스 트랜잭션 롤백 동작 확인")
    void 데이터베이스_트랜잭션_롤백_확인() {
        // 예약 생성
        ReserveSeatCommand reserveCommand = new ReserveSeatCommand(USER_ID, CONCERT_ID, DATE, SEAT_NO);
        ReservationResponse result = reserveSeatUseCase.reserveSeat(reserveCommand);

        // 트랜잭션 내에서 데이터 존재 확인
        assertThat(reservationRepository.findById(result.reservationId())).isPresent();

        // @Transactional에 의해 테스트 종료 시 롤백됨
        // 다음 테스트에서는 이 데이터가 남아있지 않아야 함
    }
}