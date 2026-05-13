package com.accioneselbosque.auth.repository;

import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.model.InvestorPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface InvestorPreferencesRepository extends JpaRepository<InvestorPreferences, UUID> {
    Optional<InvestorPreferences> findByInvestor(Investor investor);
}
