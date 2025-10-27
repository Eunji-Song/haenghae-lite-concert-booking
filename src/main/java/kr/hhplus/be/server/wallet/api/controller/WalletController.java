package kr.hhplus.be.server.wallet.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.common.security.user.CurrentUserUuid;
import kr.hhplus.be.server.wallet.api.dto.WalletBalanceResponse;
import kr.hhplus.be.server.wallet.api.dto.WalletChargeRequest;
import kr.hhplus.be.server.wallet.application.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Tag(name = "충전 계좌")
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "잔액 조회")
    @GetMapping("/balance")
    public ResponseEntity<WalletBalanceResponse> getBalance(@CurrentUserUuid String userUuid) {
        return ResponseEntity.ok(walletService.getBalance(userUuid));
    }

    @Operation(summary = "충전")
    @PostMapping("/charge")
    public ResponseEntity<WalletBalanceResponse> charge(
            @CurrentUserUuid String userUuid,
            @Valid @RequestBody WalletChargeRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return ResponseEntity.ok(walletService.charge(userUuid, request.amount(), idempotencyKey));
    }
}