package kr.hhplus.be.server.api.layered.entity.concert;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;


@Entity
@Table(name = "concert_seats")
@Getter
@SQLDelete(sql = "UPDATE concert_seats SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConcertSeatEntity {
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