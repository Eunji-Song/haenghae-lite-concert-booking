package kr.hhplus.be.server.identity.domain.model;


public record User(
        Long id,
        String userUuid,
        String email,
        String name,
        String passwordHash
) {
    /**
     * 신규 사용자 생성 시 사용하는 편의 생성자
     * (id는 null로 초기화)
     */
    public User(String userUuid, String email, String name, String passwordHash) {
        this(null, userUuid, email, name, passwordHash);
    }
}