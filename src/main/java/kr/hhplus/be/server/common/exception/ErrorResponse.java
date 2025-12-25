package kr.hhplus.be.server.common.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp
) {
    public static ErrorResponse from(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                errorCode.getCode(),
                message,
                LocalDateTime.now()
        );
    }
}