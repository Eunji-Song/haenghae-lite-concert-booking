package kr.hhplus.be.server.api.clean.interfaces.web;

import kr.hhplus.be.server.api.clean.application.port.in.PaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentUseCase paymentUseCase;

}