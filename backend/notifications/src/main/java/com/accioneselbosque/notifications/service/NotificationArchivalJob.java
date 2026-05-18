package com.accioneselbosque.notifications.service;

import com.accioneselbosque.notifications.model.Notification;
import com.accioneselbosque.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationArchivalJob {

    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "0 0 2 1 * *")
    @Transactional
    public void archiveOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(12);
        List<Notification> old = notificationRepository
                .findByStatusAndArchivedFalseAndCreatedAtBefore("SENT", cutoff);

        for (Notification n : old) {
            n.setArchived(true);
        }
        notificationRepository.saveAll(old);
        log.info("Archived {} old notifications (older than 12 months)", old.size());
    }
}
