package com.accioneselbosque.audit.service;

import com.accioneselbosque.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditArchiveJob {

    private final AuditEventRepository auditEventRepository;

    @Value("${audit.retention.years:5}")
    private int retentionYears;

    @Scheduled(cron = "0 0 2 1 * *")
    @Transactional
    public void archiveOldEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusYears(retentionYears);
        int count = auditEventRepository.archiveOlderThan(cutoff);
        log.info("AuditArchiveJob: archived {} events older than {} years", count, retentionYears);
    }
}
