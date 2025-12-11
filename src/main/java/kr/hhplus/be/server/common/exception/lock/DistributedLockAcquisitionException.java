package kr.hhplus.be.server.common.exception.lock;

/**
 * 분산락 획득 실패 시 던지는 예외.
 */
public class DistributedLockAcquisitionException extends RuntimeException {

    public DistributedLockAcquisitionException(String message) {
        super(message);
    }

    public DistributedLockAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}