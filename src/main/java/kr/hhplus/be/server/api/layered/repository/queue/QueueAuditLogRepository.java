package kr.hhplus.be.server.api.layered.repository.queue;

import kr.hhplus.be.server.api.layered.entity.queue.QueueAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueAuditLogRepository extends JpaRepository<QueueAuditLogEntity, Long> {
}