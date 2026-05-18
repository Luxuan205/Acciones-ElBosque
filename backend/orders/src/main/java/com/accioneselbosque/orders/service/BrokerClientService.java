package com.accioneselbosque.orders.service;

import com.accioneselbosque.auth.dto.ClientDetailDto;
import com.accioneselbosque.auth.dto.ClientSummaryDto;
import com.accioneselbosque.auth.model.BrokerClientAssignment;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.repository.BrokerClientAssignmentRepository;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.auth.service.BrokerIsolationGuard;
import com.accioneselbosque.orders.model.OrderStatus;
import com.accioneselbosque.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrokerClientService {

    private final BrokerClientAssignmentRepository assignmentRepository;
    private final InvestorRepository investorRepository;
    private final OrderRepository orderRepository;
    private final BrokerIsolationGuard brokerIsolationGuard;

    public List<ClientSummaryDto> getClients(Long brokerId, String search, String status, int page) {
        List<BrokerClientAssignment> assignments = assignmentRepository.findByBrokerIdAndActive(brokerId, true);

        return assignments.stream()
                .map(assignment -> {
                    Investor investor = investorRepository.findById(assignment.getInvestorId()).orElse(null);
                    if (investor == null) return null;
                    return new Object[]{ investor, assignment };
                })
                .filter(pair -> pair != null)
                .filter(pair -> {
                    Investor investor = (Investor) pair[0];
                    if (search != null && !search.isBlank()) {
                        String lower = search.toLowerCase(Locale.ROOT);
                        boolean nameMatch = investor.getFullName().toLowerCase(Locale.ROOT).contains(lower);
                        boolean emailMatch = investor.getEmail().toLowerCase(Locale.ROOT).contains(lower);
                        if (!nameMatch && !emailMatch) return false;
                    }
                    if (status != null && !status.isBlank()) {
                        if (!investor.getAccountStatus().name().equalsIgnoreCase(status)) return false;
                    }
                    return true;
                })
                .map(pair -> {
                    Investor investor = (Investor) pair[0];
                    BrokerClientAssignment assignment = (BrokerClientAssignment) pair[1];
                    long activeOrdersCount = orderRepository.countByInvestorIdAndStatusIn(
                            investor.getId(),
                            List.of(OrderStatus.PENDING, OrderStatus.QUEUED)
                    );
                    return new ClientSummaryDto(
                            investor.getId(),
                            investor.getFullName(),
                            investor.getEmail(),
                            investor.getAccountStatus().name(),
                            investor.getAvailableBalance(),
                            activeOrdersCount,
                            assignment.getAssignedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    public ClientDetailDto getClientDetail(Long brokerId, Long investorId) {
        brokerIsolationGuard.assertBrokerOwnsClient(brokerId, investorId);

        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new RuntimeException("Investor not found: " + investorId));

        BrokerClientAssignment assignment = assignmentRepository
                .findByBrokerIdAndActive(brokerId, true)
                .stream()
                .filter(a -> a.getInvestorId().equals(investorId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        return new ClientDetailDto(
                investor.getId(),
                investor.getFullName(),
                investor.getEmail(),
                investor.getPhone(),
                investor.getAccountStatus().name(),
                investor.getAvailableBalance(),
                assignment.getAssignedAt()
        );
    }
}
