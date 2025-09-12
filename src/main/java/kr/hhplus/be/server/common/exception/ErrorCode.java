package kr.hhplus.be.server.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 인증/권한 관련 (4xx)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH001", "인증이 필요합니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH002", "토큰이 만료되었습니다"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH003", "유효하지 않은 토큰입니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH004", "접근 권한이 없습니다"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH005", "이메일 또는 비밀번호가 올바르지 않습니다"),


    // 사용자 관련 (4xx)
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER001", "이미 가입된 이메일입니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER002", "사용자를 찾을 수 없습니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER003", "비밀번호가 올바르지 않습니다"),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "USER004", "이메일 형식이 올바르지 않습니다"),

    // 좌석/예약 관련 (4xx)
    SEAT_NOT_AVAILABLE(HttpStatus.CONFLICT, "SEAT001", "예약할 수 없는 좌석입니다"),
    SEAT_ALREADY_RESERVED(HttpStatus.CONFLICT, "SEAT002", "이미 예약된 좌석입니다"),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESV001", "예약을 찾을 수 없습니다"),
    RESERVATION_EXPIRED(HttpStatus.GONE, "RESV002", "예약이 만료되었습니다"),

    // 결제 관련 (4xx)
    INSUFFICIENT_BALANCE(HttpStatus.PAYMENT_REQUIRED, "PAY001", "잔액이 부족합니다"),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PAY002", "결제에 실패했습니다"),
    DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "PAY003", "중복 결제 요청입니다"),

    // 대기열 관련 (4xx)
    QUEUE_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "QUEUE001", "유효하지 않은 대기열 토큰입니다"),
    QUEUE_NOT_ACTIVE(HttpStatus.FORBIDDEN, "QUEUE002", "대기열이 활성화되지 않았습니다"),

    // 서버 오류 (5xx)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS001", "서버 내부 오류가 발생했습니다"),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS002", "데이터베이스 오류가 발생했습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}