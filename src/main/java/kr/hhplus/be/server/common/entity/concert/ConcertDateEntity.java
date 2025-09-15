package kr.hhplus.be.server.common.entity.concert;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "concert_dates")
@Getter
@Builder
@SQLDelete(sql = "UPDATE concert_dates SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ConcertDateEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concert_id", nullable = false)
    private ConcertEntity concert;

    @NotNull
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Size(max = 200)
    @NotNull
    @Column(name = "venue_name", nullable = false, length = 200)
    private String venueName;

    @NotNull
    @Column(name = "is_open", nullable = false)
    private Boolean isOpen = false;

}