package com.accioneselbosque.orders.service;

import com.accioneselbosque.auth.repository.BrokerClientAssignmentRepository;
import com.accioneselbosque.orders.exception.ClientNotAssignedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrokerAssignmentValidator {

    private final BrokerClientAssignmentRepository brokerClientAssignmentRepository;

    public void assertBrokerAssignedToClient(Long brokerId, Long clientId) {
        if (!brokerClientAssignmentRepository.existsByBrokerIdAndInvestorIdAndActive(brokerId, clientId, true)) {
            throw new ClientNotAssignedException();
        }
    }
}
