package com.accioneselbosque.notifications.repository;

import com.accioneselbosque.notifications.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByInvestorIdOrderByCreatedAtDesc(Long investorId, Pageable pageable);

    Page<Notification> findByInvestorIdAndEventTypeOrderByCreatedAtDesc(Long investorId, String eventType, Pageable pageable);

    List<Notification> findByStatusAndArchivedFalseAndCreatedAtBefore(String status, LocalDateTime cutoff);
}
