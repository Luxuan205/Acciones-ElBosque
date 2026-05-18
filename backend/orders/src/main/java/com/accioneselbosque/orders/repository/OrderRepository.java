package com.accioneselbosque.orders.repository;

import com.accioneselbosque.orders.model.Order;
import com.accioneselbosque.orders.model.OrderStatus;
import com.accioneselbosque.orders.model.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByInvestorIdOrderByCreatedAtDesc(Long investorId);

    List<Order> findByStatusAndSymbol(OrderStatus status, String symbol);

    List<Order> findByStatusIn(List<OrderStatus> statuses);

    long countByStatusIn(List<OrderStatus> statuses);

    List<Order> findByStatusAndOrderTypeIn(OrderStatus status, List<OrderType> types);

    long countByInvestorIdAndStatus(Long investorId, OrderStatus status);

    long countByInvestorIdAndStatusIn(Long investorId, List<OrderStatus> statuses);
}
