package com.accioneselbosque.notifications.repository;

import com.accioneselbosque.notifications.model.NotificationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, Long> {

    List<NotificationAttempt> findByNotificationIdOrderByAttemptNumberAsc(Long notificationId);
}
