package kr.hhplus.be.server.wallet.domain.repository;

public interface IdempotencyKeyRepository {
    boolean exists(String requestKey);
    void save(String requestKey);
}