package com.accioneselbosque.portfolio.repository;

import com.accioneselbosque.portfolio.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByInvestorIdAndExecutedAtBetween(
            Long investorId, LocalDateTime from, LocalDateTime to);

    List<Transaction> findByInvestorIdAndSymbolAndExecutedAtBetween(
            Long investorId, String symbol, LocalDateTime from, LocalDateTime to);

    long countByExecutedAtAfter(LocalDateTime executedAt);

    @Query("SELECT SUM(t.grossAmount) FROM Transaction t WHERE t.executedAt BETWEEN :from AND :to")
    BigDecimal sumGrossAmountByExecutedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT SUM(t.commission) FROM Transaction t WHERE t.executedAt BETWEEN :from AND :to")
    BigDecimal sumCommissionByExecutedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
