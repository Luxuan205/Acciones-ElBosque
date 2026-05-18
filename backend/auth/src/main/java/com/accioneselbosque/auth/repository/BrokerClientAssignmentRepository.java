package com.accioneselbosque.auth.repository;

import com.accioneselbosque.auth.model.BrokerClientAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrokerClientAssignmentRepository extends JpaRepository<BrokerClientAssignment, Long> {

    boolean existsByBrokerIdAndInvestorIdAndActive(Long brokerId, Long investorId, boolean active);

    List<BrokerClientAssignment> findByBrokerIdAndActive(Long brokerId, boolean active);
}
