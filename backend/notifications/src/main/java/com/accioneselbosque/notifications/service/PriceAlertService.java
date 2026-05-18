package com.accioneselbosque.notifications.service;

import com.accioneselbosque.auth.facade.SubscriptionGate;
import com.accioneselbosque.market_data_service.service.StockSnapshotService;
import com.accioneselbosque.notifications.dto.CreatePriceAlertRequest;
import com.accioneselbosque.notifications.dto.PriceAlertDto;
import com.accioneselbosque.notifications.exception.AlertNotModifiableException;
import com.accioneselbosque.notifications.exception.PremiumRequiredException;
import com.accioneselbosque.notifications.exception.PriceAlertNotFoundException;
import com.accioneselbosque.notifications.model.PriceAlert;
import com.accioneselbosque.notifications.model.PriceAlertStatus;
import com.accioneselbosque.notifications.model.PriceAlertType;
import com.accioneselbosque.notifications.repository.PriceAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final StockSnapshotService stockSnapshotService;
    private final SubscriptionGate subscriptionGate;

    @Transactional
    public PriceAlertDto createAlert(Long investorId, CreatePriceAlertRequest req) {
        if (!subscriptionGate.isPremiumActive(investorId)) {
            throw new PremiumRequiredException();
        }

        BigDecimal referencePrice = null;
        if (req.alertType() == PriceAlertType.PERCENTAGE) {
            referencePrice = stockSnapshotService.findBySymbol(req.symbol())
                    .map(s -> s.getCurrentPrice())
                    .orElse(null);
        }

        PriceAlert alert = PriceAlert.builder()
                .investorId(investorId)
                .symbol(req.symbol())
                .alertType(req.alertType())
                .threshold(req.threshold())
                .referencePrice(referencePrice)
                .status(PriceAlertStatus.ACTIVE)
                .build();
        alert = priceAlertRepository.save(alert);
        return toDto(alert);
    }

    @Transactional
    public void deleteAlert(Long investorId, Long alertId) {
        PriceAlert alert = priceAlertRepository.findByIdAndInvestorId(alertId, investorId)
                .orElseThrow(() -> new PriceAlertNotFoundException(alertId));
        priceAlertRepository.delete(alert);
    }

    @Transactional(readOnly = true)
    public List<PriceAlertDto> getAlerts(Long investorId) {
        return priceAlertRepository.findByInvestorIdOrderByCreatedAtDesc(investorId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PriceAlertDto updateAlert(Long investorId, Long alertId, BigDecimal newThreshold) {
        PriceAlert alert = priceAlertRepository.findByIdAndInvestorId(alertId, investorId)
                .orElseThrow(() -> new PriceAlertNotFoundException(alertId));

        if (alert.getStatus() != PriceAlertStatus.ACTIVE) {
            throw new AlertNotModifiableException(alertId);
        }

        alert.setThreshold(newThreshold);
        alert = priceAlertRepository.save(alert);
        return toDto(alert);
    }

    @Transactional
    public PriceAlertDto deactivateAlert(Long investorId, Long alertId) {
        PriceAlert alert = priceAlertRepository.findByIdAndInvestorId(alertId, investorId)
                .orElseThrow(() -> new PriceAlertNotFoundException(alertId));
        alert.setStatus(PriceAlertStatus.INACTIVE);
        alert = priceAlertRepository.save(alert);
        return toDto(alert);
    }

    @Transactional
    public PriceAlertDto reactivateAlert(Long investorId, Long alertId) {
        PriceAlert alert = priceAlertRepository.findByIdAndInvestorId(alertId, investorId)
                .orElseThrow(() -> new PriceAlertNotFoundException(alertId));

        alert.setStatus(PriceAlertStatus.ACTIVE);
        alert.setTriggeredAt(null);
        alert = priceAlertRepository.save(alert);
        return toDto(alert);
    }

    private PriceAlertDto toDto(PriceAlert a) {
        return new PriceAlertDto(
                a.getId(),
                a.getSymbol(),
                a.getAlertType().name(),
                a.getThreshold(),
                a.getReferencePrice(),
                a.getStatus().name(),
                a.getCreatedAt(),
                a.getTriggeredAt()
        );
    }
}
