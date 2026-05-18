package com.accioneselbosque.orders.repository;

import com.accioneselbosque.orders.model.CommissionRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionRateRepository extends JpaRepository<CommissionRate, String> {
}
