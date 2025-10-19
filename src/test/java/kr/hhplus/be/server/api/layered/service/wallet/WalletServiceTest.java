package kr.hhplus.be.server.api.layered.service.wallet;

import kr.hhplus.be.server.api.layered.dto.wallet.WalletBalanceResponse;
import kr.hhplus.be.server.api.layered.dto.wallet.WalletChargeRequest;
import kr.hhplus.be.server.api.layered.entity.user.UserEntity;
import kr.hhplus.be.server.api.layered.entity.wallet.WalletAccountEntity;
import kr.hhplus.be.server.api.layered.repository.wallet.WalletAccountRepository;
import kr.hhplus.be.server.api.layered.repository.wallet.WalletTransactionRepository;
import kr.hhplus.be.server.api.layered.service.user.UserService;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.common.exception.wallet.InvalidChargeAmountException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock UserService userService;
    @Mock WalletAccountRepository walletAccountRepository;
    @Mock WalletTransactionRepository walletTransactionRepository;

    @InjectMocks WalletService walletService;

    private String anyUuid() { return UUID.randomUUID().toString(); }

    private UserEntity user(long id, String uuid) {
        return UserEntity.builder()
                .id(id)
                .userUuid(uuid)
                .email("test@example.com")
                .password("{bcrypt}encoded")
                .name("테스터")
                .build();
    }

    private WalletAccountEntity account(UserEntity user, long balance) {
        return WalletAccountEntity.builder()
                .id(user.getId())
                .user(user)
                .balance(balance)
                .build();
    }

    @Test
    void 지갑이_존재하지않아_신규_지갑_생성() {
        // given
        String uuid = anyUuid();
        UserEntity u = user(1L, uuid);

        when(userService.getUser(uuid)).thenReturn(u);
        when(walletAccountRepository.findByUserId(u.getId())).thenReturn(Optional.empty());
        when(walletAccountRepository.save(any(WalletAccountEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // when
        walletService.createWallet(uuid);

        // then
        verify(userService).getUser(uuid);
        verify(walletAccountRepository).findByUserId(u.getId());
        verify(walletAccountRepository).save(any(WalletAccountEntity.class));
    }

    @Test
    void 지갑이_이미_존재하면_생성하지_않음() {
        // given
        String uuid = anyUuid();
        UserEntity u = user(2L, uuid);
        WalletAccountEntity existing = account(u, 1000L);

        when(userService.getUser(uuid)).thenReturn(u);
        when(walletAccountRepository.findByUserId(u.getId())).thenReturn(Optional.of(existing));

        // when
        walletService.createWallet(uuid);

        // then
        verify(userService).getUser(uuid);
        verify(walletAccountRepository).findByUserId(u.getId());
        verify(walletAccountRepository, never()).save(any());
    }

    @Test
    void 잔액이_0원인_계정_조회_성공() {
        // given
        String uuid = anyUuid();
        UserEntity u = user(10L, uuid);
        WalletAccountEntity a = account(u, 0L);

        when(userService.getUser(uuid)).thenReturn(u);
        when(walletAccountRepository.findByUserId(u.getId())).thenReturn(Optional.of(a));

        // when
        WalletBalanceResponse res = walletService.getBalance(uuid);

        // then
        verify(userService).getUser(uuid);
        verify(walletAccountRepository).findByUserId(u.getId());
        assertThat(res.getBalance()).isEqualTo(0L);
        assertThat(res.getCurrency()).isEqualTo("KRW");
        assertThat(res.getLastUpdatedAt()).isNotNull();
    }

    @Test
    void 잔액이_0원이_아닌_계정_조회_성공() {
        // given
        String uuid = anyUuid();
        UserEntity u = user(11L, uuid);
        WalletAccountEntity a = account(u, 123_456L);

        when(userService.getUser(uuid)).thenReturn(u);
        when(walletAccountRepository.findByUserId(u.getId())).thenReturn(Optional.of(a));

        // when
        WalletBalanceResponse res = walletService.getBalance(uuid);

        // then
        verify(userService).getUser(uuid);
        verify(walletAccountRepository).findByUserId(u.getId());

        assertThat(res.getBalance()).isEqualTo(123_456L);
        assertThat(res.getCurrency()).isEqualTo("KRW");
        assertThat(res.getLastUpdatedAt()).isNotNull();
    }

    @Test
    void 충전_성공() {
        // given
        String uuid = anyUuid();
        String idem = UUID.randomUUID().toString();

        UserEntity u = user(20L, uuid);
        WalletAccountEntity a = account(u, 50_000L);

        WalletChargeRequest request = new WalletChargeRequest(10000L);

        when(userService.getUser(uuid)).thenReturn(u);
        when(walletAccountRepository.findByUserId(u.getId())).thenReturn(Optional.of(a));
        when(walletTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        WalletBalanceResponse res = walletService.charge(uuid, request, idem);

        // then
        verify(userService).getUser(uuid);
        verify(walletAccountRepository).findByUserId(u.getId());
        verify(walletTransactionRepository).save(any());
        verify(walletAccountRepository).save(any());

        assertThat(res.getBalance()).isEqualTo(60_000L);
    }

    @Test
    void 충전실패_금액0이하() {
        // given
        String uuid = anyUuid();

        WalletChargeRequest request = new WalletChargeRequest(0L);

        // when & then
        assertThatThrownBy(() -> walletService.charge(uuid, request, "idem"))
                .isInstanceOf(InvalidChargeAmountException.class)
                .extracting(ex -> ((InvalidChargeAmountException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_AMOUNT);
    }
}