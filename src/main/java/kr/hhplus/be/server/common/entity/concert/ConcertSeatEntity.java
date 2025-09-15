package kr.hhplus.be.server.common.entity.concert;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;


@Entity
@Table(name = "concert_seats")
@Builder
@Getter
@SQLDelete(sql = "UPDATE concert_seats SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ConcertSeatEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concert_date_id", nullable = false)
    private ConcertDateEntity concertDate;

    @Column(name = "seat_no", columnDefinition = "int UNSIGNED not null")
    private Long seatNo;

    @Size(max = 50)
    @Column(name = "section", length = 50)
    private String section;

    @NotNull
    @Column(name = "price", nullable = false)
    private Long price;

}