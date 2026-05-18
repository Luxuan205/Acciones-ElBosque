package com.accioneselbosque.portfolio.facade;

import com.accioneselbosque.portfolio.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class PortfolioFacade {

    private final PositionRepository positionRepository;

    public PortfolioFacade(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    /**
     * Returns the available (current) quantity of titles an investor holds for a given symbol.
     * Returns 0 if no position exists.
     */
    public int getAvailableTitles(Long investorId, String symbol) {
        return positionRepository.findByInvestorIdAndSymbol(investorId, symbol)
                .map(p -> p.getCurrentQuantity())
                .orElse(0);
    }
}
