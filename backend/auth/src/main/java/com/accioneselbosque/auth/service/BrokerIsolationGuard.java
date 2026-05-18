package com.accioneselbosque.auth.service;

import com.accioneselbosque.auth.exception.ClientNotAssignedException;
import com.accioneselbosque.auth.repository.BrokerClientAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrokerIsolationGuard {

    private final BrokerClientAssignmentRepository assignmentRepository;

    public void assertBrokerOwnsClient(Long brokerId, Long investorId) {
        if (!assignmentRepository.existsByBrokerIdAndInvestorIdAndActive(brokerId, investorId, true)) {
            throw new ClientNotAssignedException();
        }
    }
}
