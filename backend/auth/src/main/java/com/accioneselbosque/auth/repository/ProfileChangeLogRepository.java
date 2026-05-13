package com.accioneselbosque.auth.repository;

import com.accioneselbosque.auth.model.ProfileChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProfileChangeLogRepository extends JpaRepository<ProfileChangeLog, UUID> {
    List<ProfileChangeLog> findByInvestorIdOrderByChangedAtDesc(Long investorId);
}
