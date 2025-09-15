package kr.hhplus.be.server.common.entity.concert;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "concerts")
@Builder
@Getter
@SQLDelete(sql = "UPDATE concerts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ConcertEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

    @Lob
    @Column(name = "description")
    private String description;

    @Size(max = 255)
    @Column(name = "artist_name")
    private String artistName;

    @Size(max = 255)
    @Column(name = "organizer_name")
    private String organizerName;

    @NotNull
    @Column(name = "is_open", nullable = false)
    private Boolean isOpen = false;

}