package kr.hhplus.be.server.wallet.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WalletChargeRequest(
        @NotNull @Min(1) Long amount
) {}