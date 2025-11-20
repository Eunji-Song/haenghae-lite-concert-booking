package kr.hhplus.be.server.product.infrastructure.jpa.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "concerts",
        indexes = {
                @Index(name = "idx_concerts_title", columnList = "title"),
                @Index(name = "idx_concerts_open", columnList = "is_open")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@SQLDelete(sql = "UPDATE concerts SET deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ConcertEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    private Long id;

    @Column(nullable = false, length = 255)
    @Comment("공연 제목")
    private String title;

    @Lob
    @Comment("공연 상세 설명")
    private String description;

    @Column(name = "artist_name", length = 255)
    @Comment("아티스트명")
    private String artistName;

    @Column(name = "organizer_name", length = 255)
    @Comment("주최/주관사명")
    private String organizerName;

    @Column(name = "is_open", nullable = false)
    @Comment("공개 여부(1=활성)")
    private boolean open;

    public void markOpen(boolean open) {
        this.open = open;
    }
}