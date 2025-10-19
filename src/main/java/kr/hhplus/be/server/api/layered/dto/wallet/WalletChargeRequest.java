package kr.hhplus.be.server.api.layered.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WalletChargeRequest {
    Long amount;
}
