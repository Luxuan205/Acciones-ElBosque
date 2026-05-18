package com.accioneselbosque.portfolio.service;

import com.accioneselbosque.portfolio.exception.InvalidPeriodException;
import com.accioneselbosque.portfolio.model.ReportPeriod;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class PeriodResolver {

    public LocalDate[] resolve(ReportPeriod period, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case TODAY -> new LocalDate[]{today, today};
            case WEEK -> new LocalDate[]{today.minusDays(7), today};
            case MONTH -> new LocalDate[]{today.minusDays(30), today};
            case YEAR -> new LocalDate[]{today.minusDays(365), today};
            case CUSTOM -> {
                if (from == null || to == null) {
                    throw new InvalidPeriodException("CUSTOM period requires 'from' and 'to' dates");
                }
                if (from.isAfter(to)) {
                    throw new InvalidPeriodException("'from' date must not be after 'to' date");
                }
                yield new LocalDate[]{from, to};
            }
        };
    }
}
