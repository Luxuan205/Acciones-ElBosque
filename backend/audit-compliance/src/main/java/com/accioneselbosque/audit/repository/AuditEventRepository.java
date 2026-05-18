package com.accioneselbosque.audit.repository;

import com.accioneselbosque.audit.model.AuditEvent;
import com.accioneselbosque.audit.model.AuditEventType;
import com.accioneselbosque.audit.model.AuditResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("SELECT e FROM AuditEvent e WHERE " +
           "(:investorId IS NULL OR e.investorId = :investorId) AND " +
           "(:eventType IS NULL OR e.eventType = :eventType) AND " +
           "(:result IS NULL OR e.result = :result) AND " +
           "(:from IS NULL OR e.occurredAt >= :from) AND " +
           "(:to IS NULL OR e.occurredAt <= :to) AND " +
           "(e.archived = false OR :includeArchived = true) " +
           "ORDER BY e.occurredAt DESC")
    Page<AuditEvent> findFiltered(
            @Param("investorId") Long investorId,
            @Param("eventType") AuditEventType eventType,
            @Param("result") AuditResult result,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("includeArchived") boolean includeArchived,
            Pageable pageable);

    @Modifying
    @Query("UPDATE AuditEvent e SET e.archived = true WHERE e.occurredAt < :cutoff AND e.archived = false")
    int archiveOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
