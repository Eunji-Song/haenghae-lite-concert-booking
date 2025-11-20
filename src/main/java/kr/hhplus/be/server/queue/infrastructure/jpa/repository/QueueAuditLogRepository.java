package kr.hhplus.be.server.queue.infrastructure.jpa.repository;

import kr.hhplus.be.server.queue.infrastructure.jpa.entity.QueueAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueAuditLogRepository extends JpaRepository<QueueAuditLogEntity, Long> {}