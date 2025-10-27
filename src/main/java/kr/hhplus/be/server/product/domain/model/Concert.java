package kr.hhplus.be.server.product.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Concert 도메인 모델 (엔티티 ConcertEntity 의 도메인 표현)
 */
@Getter
public class Concert {

    private final Long id;
    private final String title;
    private final String description;
    private final String artistName;
    private final String organizerName;
    private final boolean open;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Concert(Long id,
                   String title,
                   String description,
                   String artistName,
                   String organizerName,
                   boolean open,
                   LocalDateTime createdAt,
                   LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.artistName = artistName;
        this.organizerName = organizerName;
        this.open = open;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}