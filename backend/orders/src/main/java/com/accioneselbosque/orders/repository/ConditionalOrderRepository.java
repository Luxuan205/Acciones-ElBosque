package com.accioneselbosque.orders.repository;

import com.accioneselbosque.orders.model.ConditionalOrder;
import com.accioneselbosque.orders.model.ConditionalOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConditionalOrderRepository extends JpaRepository<ConditionalOrder, Long> {

    List<ConditionalOrder> findByInvestorIdAndStatus(Long investorId, ConditionalOrderStatus status);

    List<ConditionalOrder> findBySymbolAndStatus(String symbol, ConditionalOrderStatus status);

    List<ConditionalOrder> findByStatus(ConditionalOrderStatus status);
}
