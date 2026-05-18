package com.accioneselbosque.portfolio.service;

import com.accioneselbosque.portfolio.dto.PortfolioReportDto;
import com.accioneselbosque.portfolio.dto.PositionDto;
import com.accioneselbosque.portfolio.dto.TransactionDto;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class CsvReportExporter {

    public byte[] export(PortfolioReportDto report) {
        StringBuilder sb = new StringBuilder();

        sb.append("OPEN POSITIONS\n");
        sb.append("symbol,quantity,avgPrice,currentPrice,unrealizedGain\n");
        for (PositionDto pos : report.positions()) {
            sb.append(pos.symbol()).append(",")
              .append(pos.quantity()).append(",")
              .append(pos.avgBuyPrice()).append(",")
              .append(pos.currentPrice()).append(",")
              .append(pos.unrealizedGain()).append("\n");
        }

        sb.append("\nTRANSACTIONS\n");
        sb.append("type,symbol,qty,executionPrice,commission,netAmount,realizedGain,executedAt\n");
        for (TransactionDto tx : report.transactions()) {
            sb.append(tx.transactionType()).append(",")
              .append(tx.symbol()).append(",")
              .append(tx.quantity()).append(",")
              .append(tx.executionPrice()).append(",")
              .append(tx.commission()).append(",")
              .append(tx.netAmount()).append(",")
              .append(tx.realizedGain() != null ? tx.realizedGain() : "").append(",")
              .append(tx.executedAt()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
