package com.accioneselbosque.auth.repository;

import com.accioneselbosque.auth.model.Investor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InvestorRepository extends JpaRepository<Investor, Long> {

    Optional<Investor> findByEmail(String email);

    Optional<Investor> findByDocumentNumber(String documentNumber);

    boolean existsByEmail(String email);

    boolean existsByDocumentNumber(String documentNumber);
}
