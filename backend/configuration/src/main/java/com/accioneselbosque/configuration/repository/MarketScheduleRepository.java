package com.accioneselbosque.configuration.repository;

import com.accioneselbosque.configuration.model.MarketSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketScheduleRepository extends JpaRepository<MarketSchedule, UUID> {

    default Optional<MarketSchedule> findFirst() {
        return findAll().stream().findFirst();
    }
}
