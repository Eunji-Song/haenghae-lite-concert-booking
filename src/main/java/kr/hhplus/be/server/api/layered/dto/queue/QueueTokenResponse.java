package kr.hhplus.be.server.api.layered.dto.queue;

import jakarta.persistence.criteria.CriteriaBuilder;
import kr.hhplus.be.server.common.enums.QueueStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter

@AllArgsConstructor
@NoArgsConstructor
public class QueueTokenResponse {
    String queueToken;
    QueueStatus status;
    Long rank;
    Long etaSeconds;


    public QueueTokenResponse(String queueToken, QueueStatus status) {
        this.queueToken = queueToken;
        this.status = status;
        this.rank = null;
        this.etaSeconds = null;
    }
}
