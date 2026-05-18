package com.accioneselbosque.notifications.service;

import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.repository.InvestorPreferencesRepository;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.notifications.dto.NotificationAttemptDto;
import com.accioneselbosque.notifications.dto.NotificationDetailDto;
import com.accioneselbosque.notifications.dto.NotificationDto;
import com.accioneselbosque.notifications.dto.PagedNotificationResponse;
import com.accioneselbosque.notifications.exception.NotificationNotFoundException;
import com.accioneselbosque.notifications.model.Notification;
import com.accioneselbosque.notifications.model.NotificationAttempt;
import com.accioneselbosque.notifications.model.NotificationEventType;
import com.accioneselbosque.notifications.model.OrderStatusChangeEvent;
import com.accioneselbosque.notifications.repository.NotificationAttemptRepository;
import com.accioneselbosque.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository notificationAttemptRepository;
    private final InvestorPreferencesRepository investorPreferencesRepository;
    private final InvestorRepository investorRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void sendOrderStatusChange(OrderStatusChangeEvent event) {
        String channel = resolveChannel(event.getInvestorId());
        String subject = buildSubject(event);
        String body = buildBody(event);

        if ("BOTH".equals(channel)) {
            createAndSendNotification(event.getInvestorId(), event.getEventType(), "EMAIL",
                    subject, body, event.getOrderId());
            createAndSendNotification(event.getInvestorId(), event.getEventType(), "PUSH",
                    subject, body, event.getOrderId());
        } else {
            createAndSendNotification(event.getInvestorId(), event.getEventType(), channel,
                    subject, body, event.getOrderId());
        }
    }

    private String resolveChannel(Long investorId) {
        return investorRepository.findById(investorId)
                .flatMap(investorPreferencesRepository::findByInvestor)
                .map(prefs -> {
                    com.accioneselbosque.auth.model.NotifChannel authChannel = prefs.getNotifChannel();
                    if (authChannel == null) return "EMAIL";
                    switch (authChannel) {
                        case EMAIL: return "EMAIL";
                        case SMS:   return "PUSH";
                        case NONE:  return "EMAIL";
                        default:    return "EMAIL";
                    }
                })
                .orElse("EMAIL");
    }

    private String buildSubject(OrderStatusChangeEvent event) {
        NotificationEventType type = event.getEventType();
        if (type == null) return "Notificación de orden";
        return switch (type) {
            case ORDER_EXECUTED  -> "Orden ejecutada: " + event.getStockSymbol();
            case ORDER_CANCELLED -> "Orden cancelada: " + event.getStockSymbol();
            case ORDER_REJECTED  -> "Orden rechazada: " + event.getStockSymbol();
            case ORDER_QUEUED    -> "Orden en cola: " + event.getStockSymbol();
            default              -> "Notificación de orden";
        };
    }

    private String buildBody(OrderStatusChangeEvent event) {
        NotificationEventType type = event.getEventType();
        if (type == null) return "Actualización de su orden.";
        return switch (type) {
            case ORDER_EXECUTED -> String.format(
                    "Su orden de %d acciones de %s fue ejecutada a $%s. Comisión: $%s. Total: $%s.",
                    event.getQuantity(), event.getStockSymbol(),
                    event.getExecutionPrice(), event.getCommission(), event.getTotalAmount());
            case ORDER_CANCELLED -> String.format(
                    "Su orden de %d acciones de %s fue cancelada. Motivo: %s.",
                    event.getQuantity(), event.getStockSymbol(), event.getCancellationReason());
            case ORDER_REJECTED -> String.format(
                    "Su orden de %d acciones de %s fue rechazada. Motivo: %s.",
                    event.getQuantity(), event.getStockSymbol(), event.getRejectionReason());
            case ORDER_QUEUED -> String.format(
                    "Su orden de %d acciones de %s ha sido encolada para procesamiento.",
                    event.getQuantity(), event.getStockSymbol());
            default -> "Actualización de su orden.";
        };
    }

    void createAndSendNotification(Long investorId, NotificationEventType eventType,
                                   String channel, String subject, String body, Long referenceId) {
        Notification notification = Notification.builder()
                .investorId(investorId)
                .eventType(eventType != null ? eventType.name() : "UNKNOWN")
                .channel(channel)
                .subject(subject)
                .body(body)
                .status("PENDING")
                .referenceId(referenceId)
                .build();
        notification = notificationRepository.save(notification);

        // Simulate sending
        if ("EMAIL".equals(channel)) {
            log.info("EMAIL sent: {}", subject);
        } else {
            log.info("PUSH stub: skipped — subject: {}", subject);
        }

        notification.setStatus("SENT");
        notificationRepository.save(notification);

        NotificationAttempt attempt = NotificationAttempt.builder()
                .notificationId(notification.getId())
                .attemptNumber(1)
                .status("SUCCESS")
                .build();
        notificationAttemptRepository.save(attempt);
    }

    @Transactional(readOnly = true)
    public PagedNotificationResponse getHistory(Long investorId, int page, int size, String eventType) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Notification> result;
        if (eventType != null && !eventType.isBlank()) {
            result = notificationRepository.findByInvestorIdAndEventTypeOrderByCreatedAtDesc(
                    investorId, eventType, pageRequest);
        } else {
            result = notificationRepository.findByInvestorIdOrderByCreatedAtDesc(investorId, pageRequest);
        }

        List<NotificationDto> content = result.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new PagedNotificationResponse(content, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public NotificationDetailDto getDetail(Long investorId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getInvestorId().equals(investorId))
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        List<NotificationAttemptDto> attempts = notificationAttemptRepository
                .findByNotificationIdOrderByAttemptNumberAsc(notificationId)
                .stream()
                .map(a -> new NotificationAttemptDto(a.getAttemptNumber(), a.getStatus(), a.getAttemptedAt()))
                .collect(Collectors.toList());

        return new NotificationDetailDto(
                notification.getId(),
                notification.getEventType(),
                notification.getChannel(),
                notification.getSubject(),
                notification.getBody(),
                notification.getStatus(),
                notification.getReferenceId(),
                notification.getCreatedAt(),
                attempts
        );
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(), n.getEventType(), n.getChannel(),
                n.getSubject(), n.getBody(), n.getStatus(),
                n.getReferenceId(), n.getCreatedAt()
        );
    }
}
