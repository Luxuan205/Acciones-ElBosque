package com.accioneselbosque.notifications.service;

import com.accioneselbosque.notifications.model.Notification;
import com.accioneselbosque.notifications.model.NotificationAttempt;
import com.accioneselbosque.notifications.model.PriceAlertTriggeredEvent;
import com.accioneselbosque.notifications.repository.NotificationAttemptRepository;
import com.accioneselbosque.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceAlertEventListener {

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository notificationAttemptRepository;

    @EventListener
    @Transactional
    public void onPriceAlertTriggered(PriceAlertTriggeredEvent event) {
        String subject = "Alerta de precio activada: " + event.getSymbol();
        String body = String.format(
                "Su alerta de precio para %s ha sido activada. Precio actual: $%s. Umbral configurado: %s %s.",
                event.getSymbol(),
                event.getCurrentPrice(),
                event.getThreshold(),
                event.getAlertType().name()
        );

        Notification notification = Notification.builder()
                .investorId(event.getInvestorId())
                .eventType("PRICE_ALERT")
                .channel("EMAIL")
                .subject(subject)
                .body(body)
                .status("SENT")
                .referenceId(event.getAlertId())
                .build();
        notification = notificationRepository.save(notification);

        NotificationAttempt attempt = NotificationAttempt.builder()
                .notificationId(notification.getId())
                .attemptNumber(1)
                .status("SUCCESS")
                .build();
        notificationAttemptRepository.save(attempt);

        log.info("Price alert notification created for investor {} — symbol {}", event.getInvestorId(), event.getSymbol());
    }
}
