package kr.hhplus.be.server.api.clean.interfaces.web;

import kr.hhplus.be.server.api.clean.application.port.in.ReservationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationUseCase reservationUseCase;

}