package com.accioneselbosque.notifications.service;

import com.accioneselbosque.notifications.model.MarketAlertSubscription;
import com.accioneselbosque.notifications.model.MarketEvent;
import com.accioneselbosque.notifications.model.Notification;
import com.accioneselbosque.notifications.model.NotificationAttempt;
import com.accioneselbosque.notifications.repository.MarketAlertSubscriptionRepository;
import com.accioneselbosque.notifications.repository.NotificationAttemptRepository;
import com.accioneselbosque.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketAlertDispatcher {

    private final MarketAlertSubscriptionRepository marketAlertSubscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository notificationAttemptRepository;

    @EventListener
    @Transactional
    public void onMarketEvent(MarketEvent event) {
        List<MarketAlertSubscription> subscriptions =
                marketAlertSubscriptionRepository.findByAlertTypeAndActive(event.getAlertType(), true);

        int count = 0;
        for (MarketAlertSubscription subscription : subscriptions) {
            String subject = "Alerta de mercado: " + event.getAlertType().name();
            String body = event.getDescription() != null
                    ? event.getDescription()
                    : "Se ha producido una alerta de mercado de tipo " + event.getAlertType().name();

            Notification notification = Notification.builder()
                    .investorId(subscription.getInvestorId())
                    .eventType("MARKET_ALERT")
                    .channel("EMAIL")
                    .subject(subject)
                    .body(body)
                    .status("SENT")
                    .build();
            notification = notificationRepository.save(notification);

            NotificationAttempt attempt = NotificationAttempt.builder()
                    .notificationId(notification.getId())
                    .attemptNumber(1)
                    .status("SUCCESS")
                    .build();
            notificationAttemptRepository.save(attempt);
            count++;
        }

        log.info("Market alert dispatched to {} investors for event type {}", count, event.getAlertType());
    }
}
