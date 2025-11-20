package kr.hhplus.be.server.payment.integration;

import kr.hhplus.be.server.common.integration.BaseIntegrationTest;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.payment.application.port.out.WalletPort;
import kr.hhplus.be.server.wallet.domain.model.WalletAccount;
import kr.hhplus.be.server.wallet.domain.repository.WalletAccountRepository;
import kr.hhplus.be.server.wallet.infrastructure.jpa.entity.WalletAccountEntity;
import kr.hhplus.be.server.wallet.infrastructure.jpa.repository.WalletAccountJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WalletDebitConcurrencyTest extends BaseIntegrationTest {

    @Autowired
    private WalletPort walletPort;

    @Autowired
    private UserJpaRepository userRepo;

    @Autowired
    private WalletAccountJpaRepository walletAccountJpaRepo;

    @Autowired
    private WalletAccountRepository walletAccountRepo;

    @Test
    @DisplayName("동시에 같은 지갑을 차감해도 잔액이 음수가 되지 않고, 가능한 횟수만 성공한다")
    void concurrent_debit_should_not_overdraw_wallet() throws Exception {
        // ----- given: 유저 + 지갑 생성 -----
        UserEntity user = userRepo.save(UserEntity.builder()
                .userUuid("wallet-user-1")
                .email("wallet@test.com")
                .password("{noop}p")
                .name("wallet-user")
                .build());

        Long userId = user.getId();

        // 지갑 계좌 생성 + 초기 잔액 세팅 (100000원)
        WalletAccount account = walletAccountRepo.createIfNotExists(userId);
        account.charge(100000L);
        walletAccountRepo.save(account);

        long initialBalance = account.getBalance();
        long debitAmount = 30000L;
        int threads = 10;

        // 동시에 실행될 스레드 풀 생성 (threads 만큼 고정된 스레드가 돌아감)
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        // 모든 스레드가 동시에 시작하도록 대기시키는 래치
        // start.countDown() 이 호출되기 전까지 모든 스레드는 start.await() 에서 멈춤
        CountDownLatch start = new CountDownLatch(1);

        // 모든 스레드가 작업을 끝냈는지 기다리기 위한 래치
        // 스레드 하나 끝날 때마다 done.countDown(), done.await() 로 전체 완료 대기
        CountDownLatch done = new CountDownLatch(threads);

        // 성공한 스레드 개수를 원자적으로 카운트 (스레드 간 경합에도 안전)
        AtomicInteger success = new AtomicInteger();

        // 실패한 스레드 개수를 원자적으로 카운트 (예외 발생 시 증가)
        AtomicInteger fail = new AtomicInteger();

        List<Throwable> errors = new ArrayList<>();

        // ----- when: 동시에 차감 시도 -----

        // 여러 스레드에게 동일 작업을 시키기 위해 threads 번 반복
        for (int i = 0; i < threads; i++) {
            // 람다에서 캡처할 때 i 값이 변하지 않도록 final 로 복사
            final int idx = i;

            // 스레드 풀에 작업 제출 (아직 실행은 안 될 수 있고, 큐에 쌓임)
            pool.submit(() -> {
                try {
                    // 여기서 모두 start.await() 에서 대기하다가메인 스레드가 start.countDown() 하는 순간 한꺼번에 깨어남
                    start.await();

                    try {
                        // 각 스레드마다 다른 멱등키 사용 (동시성만 검증하고, 멱등 중복은 피하려고)
                        String idemKey = "concurrent-debit-" + idx;

                        // 실제 “동시에 실행되기를 의도한” 코드
                        //    - 여러 스레드가 같은 userId 지갑을 동시에 차감 요청
                        //    - 내부에서 JPA로 wallet_accounts 를 조회 + PESSIMISTIC_WRITE 락 획득
                        //    - 잔액 검증 후 debit 수행
                        walletPort.debit(userId, debitAmount, null, idemKey);

                        // 예외 없이 여기까지 오면 “성공 처리된 차감”으로 카운트
                        success.incrementAndGet();
                    } catch (Throwable t) {
                        // walletPort.debit 내에서 예외(잔액 부족, 락 충돌 등) 발생 시 실패 카운트 증가
                        fail.incrementAndGet();

                        // 어떤 예외들이 났는지 나중에 보고 디버깅할 수 있도록 리스트에 모아둠
                        synchronized (errors) {
                            errors.add(t);
                        }
                    }
                } catch (InterruptedException ignored) {
                    // start.await() 가 인터럽트된 경우지만, 테스트에서는 크게 중요하지 않으니 무시
                } finally {
                    // 이 스레드의 작업이 끝났음을 알림 → done.await() 에서 대기 중인 메인 스레드에게 신호
                    done.countDown();
                }
            });
        }

        // 여기서 start.countDown() 이 호출되는 순간 위에서 start.await() 에 막혀 있던 모든 스레드가 "거의 동시에" 풀려 나감
        start.countDown();

        // done.await(...) 는 모든 스레드가 done.countDown() 으로 완료 신호를 보내줄 때까지 대기 (혹은 타임아웃 20초가 지나면 false 반환)
        boolean finished = done.await(20, TimeUnit.SECONDS);

        // 더 이상 쓸 일 없는 스레드 풀은 바로 종료 요청
        pool.shutdownNow();

        // ----- then: 검증 -----
        assertThat(finished).as("모든 작업이 제한 시간 내 끝났는지").isTrue();

        // -----------------------------
        // 최종 잔액 및 성공 횟수 검사
        // -----------------------------
        WalletAccountEntity after = walletAccountJpaRepo.findByUserId(userId)
                .orElseThrow();

        int expectedMaxSuccess = (int) (initialBalance / debitAmount);

        assertThat(success.get())
                .as("성공 횟수는 가능한 최대 횟수(%d)를 넘지 않아야 함", expectedMaxSuccess)
                .isLessThanOrEqualTo(expectedMaxSuccess);

        assertThat(fail.get())
                .as("실패 횟수 = 전체 시도 - 성공 시도")
                .isEqualTo(threads - success.get());

        assertThat(after.getBalance())
                .as("잔액은 음수가 될 수 없다")
                .isGreaterThanOrEqualTo(0L);

        long expectedBalance = initialBalance - ((long) success.get() * debitAmount);
        assertThat(after.getBalance())
                .as("최종 잔액은 초기 잔액 - (성공횟수 * 차감금액)")
                .isEqualTo(expectedBalance);

        if (!errors.isEmpty()) {
            errors.forEach(Throwable::printStackTrace);
        }
    }
}