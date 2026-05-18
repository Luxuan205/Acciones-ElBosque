package com.accioneselbosque.configuration.repository;

import com.accioneselbosque.configuration.model.ParameterChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParameterChangeHistoryRepository extends JpaRepository<ParameterChangeHistory, Long> {
    List<ParameterChangeHistory> findByParameterKeyOrderByChangedAtDesc(String key);
    Optional<ParameterChangeHistory> findTopByParameterKeyOrderByChangedAtDesc(String key);
}
