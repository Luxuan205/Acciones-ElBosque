package com.accioneselbosque.portfolio.repository;

import com.accioneselbosque.portfolio.model.Transaction;
import com.accioneselbosque.portfolio.model.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class TransactionSpecification {

    public static Specification<Transaction> withFilters(
            LocalDate from, LocalDate to, Long investorId, String symbol, String type) {

        return Specification
                .where(fromDate(from))
                .and(toDate(to))
                .and(byInvestorId(investorId))
                .and(bySymbol(symbol))
                .and(byType(type));
    }

    private static Specification<Transaction> fromDate(LocalDate from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.get("executedAt"), from.atStartOfDay());
    }

    private static Specification<Transaction> toDate(LocalDate to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThan(root.get("executedAt"), to.plusDays(1).atStartOfDay());
    }

    private static Specification<Transaction> byInvestorId(Long investorId) {
        return (root, query, cb) -> investorId == null ? null
                : cb.equal(root.get("investorId"), investorId);
    }

    private static Specification<Transaction> bySymbol(String symbol) {
        return (root, query, cb) -> (symbol == null || symbol.isBlank()) ? null
                : cb.like(cb.lower(root.get("symbol")), "%" + symbol.trim().toLowerCase() + "%");
    }

    private static Specification<Transaction> byType(String type) {
        if (type == null || type.isBlank()) return null;
        try {
            TransactionType tt = TransactionType.valueOf(type.trim().toUpperCase());
            return (root, query, cb) -> cb.equal(root.get("transactionType"), tt);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
