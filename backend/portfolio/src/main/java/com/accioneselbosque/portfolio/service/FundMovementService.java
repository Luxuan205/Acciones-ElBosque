package com.accioneselbosque.portfolio.service;

import com.accioneselbosque.portfolio.dto.FundMovementDto;
import com.accioneselbosque.portfolio.dto.FundMovementPageResponse;
import com.accioneselbosque.portfolio.model.FundMovement;
import com.accioneselbosque.portfolio.repository.FundMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class FundMovementService {

    private final FundMovementRepository fundMovementRepository;

    @Transactional(readOnly = true)
    public FundMovementPageResponse getMovements(Long investorId, LocalDate from, LocalDate to, int page) {
        PageRequest pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending());

        Page<FundMovement> result;
        if (from == null || to == null) {
            result = fundMovementRepository.findByInvestorId(investorId, pageable);
        } else {
            result = fundMovementRepository.findByInvestorIdAndCreatedAtBetween(
                    investorId,
                    from.atStartOfDay(),
                    to.atTime(LocalTime.of(23, 59, 59)),
                    pageable
            );
        }

        return new FundMovementPageResponse(
                result.getContent().stream().map(this::toDto).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    private FundMovementDto toDto(FundMovement m) {
        return new FundMovementDto(
                m.getId(),
                m.getType().name(),
                m.getAmount(),
                m.getBalanceAfter(),
                m.getCurrency(),
                m.getDescription(),
                m.getOrderId(),
                m.getCreatedAt()
        );
    }
}
