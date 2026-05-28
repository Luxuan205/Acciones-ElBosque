package com.accioneselbosque.app.service;

import com.accioneselbosque.app.dto.AdminLinkDto;
import com.accioneselbosque.app.dto.AdminTransactionDto;
import com.accioneselbosque.app.dto.DashboardPeriod;
import com.accioneselbosque.app.dto.FinancialSummaryDto;
import com.accioneselbosque.app.dto.OperationalMetricsDto;
import com.accioneselbosque.auth.model.AccountStatus;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.model.SubscriptionType;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.configuration.service.MarketStatusService;
import com.accioneselbosque.orders.model.OrderStatus;
import com.accioneselbosque.orders.repository.OrderRepository;
import com.accioneselbosque.portfolio.model.Transaction;
import com.accioneselbosque.portfolio.repository.TransactionRepository;
import com.accioneselbosque.portfolio.repository.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MarketStatusService marketStatusService;
    private final OrderRepository orderRepository;
    private final InvestorRepository investorRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public OperationalMetricsDto getOperationalMetrics() {
        String marketStatus = marketStatusService.isMarketOpen() ? "OPEN" : "CLOSED";
        long activeOrders = orderRepository.countByStatusIn(List.of(OrderStatus.PENDING, OrderStatus.QUEUED));
        long connectedUsers = investorRepository.countByAccountStatus(AccountStatus.ACTIVE);
        long todayTransactions = transactionRepository.countByExecutedAtAfter(LocalDate.now().atStartOfDay());
        return new OperationalMetricsDto(marketStatus, activeOrders, connectedUsers, todayTransactions, 0L);
    }

    @Transactional(readOnly = true)
    public FinancialSummaryDto getFinancialSummary(DashboardPeriod period) {
        LocalDate today = LocalDate.now();
        LocalDate from = switch (period) {
            case TODAY -> today;
            case WEEK -> today.minusDays(7);
            case MONTH -> today.minusDays(30);
        };

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = today.plusDays(1).atStartOfDay();

        BigDecimal totalVolume = transactionRepository.sumGrossAmountByExecutedAtBetween(fromDt, toDt);
        if (totalVolume == null) totalVolume = BigDecimal.ZERO;

        BigDecimal commissionRevenue = transactionRepository.sumCommissionByExecutedAtBetween(fromDt, toDt);
        if (commissionRevenue == null) commissionRevenue = BigDecimal.ZERO;

        long newRegistrations = investorRepository.countByCreatedAtBetween(fromDt, toDt);
        long activePremium = investorRepository.countBySubscriptionTypeAndSubscriptionExpiresAtAfter(
                SubscriptionType.PREMIUM, LocalDateTime.now());

        return new FinancialSummaryDto(period, from, today, totalVolume, commissionRevenue,
                newRegistrations, activePremium);
    }

    public List<AdminLinkDto> getAdminLinks() {
        return List.of(
                new AdminLinkDto("Gestión de Usuarios", "/admin/users",
                        "Buscar, suspender y gestionar roles de usuarios"),
                new AdminLinkDto("Configuración de Mercados", "/config/markets",
                        "Horarios y feriados del mercado"),
                new AdminLinkDto("Log de Auditoría", "/audit/events",
                        "Registro completo de eventos del sistema"),
                new AdminLinkDto("Parámetros Globales", "/config/parameters",
                        "Configuración de parámetros del sistema")
        );
    }

    @Transactional(readOnly = true)
    public Page<AdminTransactionDto> getAdminTransactions(
            LocalDate from, LocalDate to, Long investorId,
            String symbol, String type, Pageable pageable) {

        Specification<Transaction> spec =
                TransactionSpecification.withFilters(from, to, investorId, symbol, type);

        Page<Transaction> page = transactionRepository.findAll(spec, pageable);

        Set<Long> investorIds = page.getContent().stream()
                .map(Transaction::getInvestorId)
                .collect(Collectors.toSet());

        Map<Long, Investor> investors = investorRepository.findAllById(investorIds).stream()
                .collect(Collectors.toMap(Investor::getId, Function.identity()));

        return page.map(t -> {
            Investor inv = investors.get(t.getInvestorId());
            String name  = inv != null ? inv.getFullName() : "—";
            String email = inv != null ? inv.getEmail()    : "—";
            return new AdminTransactionDto(
                    t.getId(), name, email,
                    t.getSymbol(),
                    t.getTransactionType().name(),
                    t.getQuantity(),
                    t.getGrossAmount(),
                    t.getCommission(),
                    t.getExecutedAt());
        });
    }
}
