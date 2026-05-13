package com.accioneselbosque.configuration.repository;

import com.accioneselbosque.configuration.model.MarketHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketHolidayRepository extends JpaRepository<MarketHoliday, UUID> {

    Optional<MarketHoliday> findByDate(LocalDate date);

    boolean existsByDate(LocalDate date);

    List<MarketHoliday> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);
}
