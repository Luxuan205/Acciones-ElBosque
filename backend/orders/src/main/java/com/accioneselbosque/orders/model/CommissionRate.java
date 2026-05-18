package com.accioneselbosque.orders.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "commission_rate")
@Getter
@NoArgsConstructor
public class CommissionRate {

    @Id
    @Column(name = "subscription_type", length = 20)
    private String subscriptionType;

    @Column(name = "rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal ratePercent;
}
