package kr.hhplus.be.server.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
      비즈니스 예외 처리
      - BaseException을 상속한 모든 예외를 ErrorCode 기반 응답으로 변환
    */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.from(errorCode, e.getMessage()));
    }

    /*
      Validation 예외 처리 (@Valid)
      - 프로젝트에서 ErrorCode를 INVALID_* 형태로 이미 갖고 있으니,
        필요 시 도메인별로 매핑하거나 공통 BAD_REQUEST로 처리
    */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다");

        ErrorCode errorCode = ErrorCode.INVALID_AMOUNT; // 공통 BAD_REQUEST 코드가 따로 없어서 임시 매핑
        // 공통 VALIDATION ErrorCode를 추가하는 편이 가장 깔끔함 (예: SYS003 VALIDATION_ERROR)

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.from(errorCode, message));
    }

    /*
      예상하지 못한 예외 처리
    */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.from(errorCode, errorCode.getMessage()));
    }
}