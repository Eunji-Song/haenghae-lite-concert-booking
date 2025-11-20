package kr.hhplus.be.server.wallet.application.service;

import kr.hhplus.be.server.identity.domain.model.User;
import kr.hhplus.be.server.identity.domain.repository.UserRepository;
import kr.hhplus.be.server.wallet.api.dto.WalletBalanceResponse;
import kr.hhplus.be.server.wallet.domain.model.WalletAccount;
import kr.hhplus.be.server.wallet.domain.model.WalletTransaction;
import kr.hhplus.be.server.wallet.domain.repository.IdempotencyKeyRepository;
import kr.hhplus.be.server.wallet.domain.repository.WalletAccountRepository;
import kr.hhplus.be.server.wallet.domain.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WalletService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletAccountRepository walletAccountRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private IdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private WalletService walletService;

    private User user;
    private final String userUuid = "u-1234-uuid";
    private final Long userId = 10L;

    @BeforeEach
    void setUp() {
        user = new User(
                userId,
                userUuid,
                "user@test.com",
                "Tester",
                "$2a$hash"   // passwordHash
        );
    }

    @Test
    void getBalance_createsAccountIfMissing_andReturnsCreatedAtWhenNoTx() {
        // given
        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));

        var createdAt = LocalDateTime.of(2025, 10, 1, 12, 0);
        var newAccount = new WalletAccount(userId, 0L, createdAt);

        when(walletAccountRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(walletAccountRepository.createIfNotExists(userId)).thenReturn(newAccount);
        when(walletTransactionRepository.findLatestCreatedAtByUserId(userId)).thenReturn(Optional.empty());

        // when
        WalletBalanceResponse res = walletService.getBalance(userUuid);

        // then
        assertThat(res.balance()).isEqualTo(0L);
        assertThat(res.currency()).isEqualTo("KRW");
        assertThat(res.lastUpdatedAt()).isEqualTo(createdAt);

        verify(userRepository).findByUuid(userUuid);
        verify(walletAccountRepository).createIfNotExists(userId);
        verify(walletTransactionRepository).findLatestCreatedAtByUserId(userId);
        verifyNoMoreInteractions(walletTransactionRepository, idempotencyKeyRepository);
    }

    @Test
    void charge_firstTime_succeeds_updatesBalance_andPersistsTransaction_andIdempotencyKey() {
        // given
        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));

        var createdAt = LocalDateTime.of(2025, 10, 1, 12, 0);
        var account = new WalletAccount(userId, 5_000L, createdAt); // 초기 잔액 5,000
        var idempotencyKey = "idem-abc";
        var amount = 7_000L;

        when(idempotencyKeyRepository.exists(idempotencyKey)).thenReturn(false);
        when(walletAccountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        // charge 이후 저장되는 account 캡처
        ArgumentCaptor<WalletAccount> savedAccountCaptor = ArgumentCaptor.forClass(WalletAccount.class);
        doAnswer(invocation -> invocation.getArgument(0)).when(walletAccountRepository).save(savedAccountCaptor.capture());

        // Tx 저장 캡처
        ArgumentCaptor<WalletTransaction> savedTxnCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        doAnswer(invocation -> invocation.getArgument(0)).when(walletTransactionRepository).save(savedTxnCaptor.capture());

        // 마지막 거래시각 조회 스텁
        var lastTxAt = LocalDateTime.of(2025, 10, 2, 9, 30);
        when(walletTransactionRepository.findLatestCreatedAtByUserId(userId)).thenReturn(Optional.of(lastTxAt));

        // when
        WalletBalanceResponse res = walletService.charge(userUuid, amount, idempotencyKey);

        // then
        // response
        assertThat(res.balance()).isEqualTo(12_000L); // 5,000 + 7,000
        assertThat(res.currency()).isEqualTo("KRW");
        assertThat(res.lastUpdatedAt()).isEqualTo(lastTxAt);

        // 저장된 Account 검증
        WalletAccount savedAccount = savedAccountCaptor.getValue();
        assertThat(savedAccount.getUserId()).isEqualTo(userId);
        assertThat(savedAccount.getBalance()).isEqualTo(12_000L);

        // 저장된 Tx 검증
        WalletTransaction savedTxn = savedTxnCaptor.getValue();
        assertThat(savedTxn.getUserId()).isEqualTo(userId);
        assertThat(savedTxn.getAmount()).isEqualTo(amount);
        assertThat(savedTxn.getIdempotencyKey()).isEqualTo(idempotencyKey);
        // 타입은 WalletTransaction.charge(...) 가 CHARGE 로 생성한다고 가정

        verify(idempotencyKeyRepository).exists(idempotencyKey);
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
        verify(idempotencyKeyRepository).save(idempotencyKey);
    }

    @Test
    void charge_idempotentKeyReused_returnsCurrentBalance_withoutMutations() {
        // given
        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));

        var createdAt = LocalDateTime.of(2025, 10, 1, 12, 0);
        var account = new WalletAccount(userId, 10_000L, createdAt);
        var idempotencyKey = "idem-dup";

        when(idempotencyKeyRepository.exists(idempotencyKey)).thenReturn(true);
        when(walletAccountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(walletTransactionRepository.findLatestCreatedAtByUserId(userId)).thenReturn(Optional.empty());

        // when
        WalletBalanceResponse res = walletService.charge(userUuid, 3_000L, idempotencyKey);

        // then: 변경 없음
        assertThat(res.balance()).isEqualTo(10_000L);
        assertThat(res.currency()).isEqualTo("KRW");
        assertThat(res.lastUpdatedAt()).isEqualTo(createdAt); // 거래 없었으므로 account.createdAt

        // 멱등 재사용 시, 저장 동작이 없어야 함
        verify(walletAccountRepository, never()).save(any());
        verify(walletTransactionRepository, never()).save(any());
        verify(idempotencyKeyRepository, never()).save(anyString());
    }
}