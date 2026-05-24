package com.accioneselbosque.audit.repository;

import com.accioneselbosque.audit.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditEventRepository
        extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

    @Modifying
    @Query("UPDATE AuditEvent e SET e.archived = true WHERE e.occurredAt < :cutoff AND e.archived = false")
    int archiveOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
