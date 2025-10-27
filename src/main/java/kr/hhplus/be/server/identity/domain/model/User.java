package kr.hhplus.be.server.identity.domain.model;


import lombok.Getter;

@Getter
public class User {
    private final Long id;
    private final String userUuid;
    private final String email;
    private final String name;
    private final String passwordHash;

    public User(String userUuid, String email, String name, String passwordHash) {
        this(null, userUuid, email, name, passwordHash);
    }

    public User(Long id, String userUuid, String email, String name, String passwordHash) {
        this.id = id;
        this.userUuid = userUuid;
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
    }
}