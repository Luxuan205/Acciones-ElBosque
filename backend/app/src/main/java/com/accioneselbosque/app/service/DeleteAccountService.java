package com.accioneselbosque.app.service;

import com.accioneselbosque.auth.exception.CannotDeleteAccountException;
import com.accioneselbosque.auth.model.AccountStatus;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.audit.model.AuditEventRecord;
import com.accioneselbosque.audit.model.AuditEventType;
import com.accioneselbosque.audit.model.AuditResult;
import com.accioneselbosque.audit.service.AuditService;
import com.accioneselbosque.portfolio.repository.AccountBalanceRepository;
import com.accioneselbosque.portfolio.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DeleteAccountService {

    private final InvestorRepository investorRepository;
    private final PositionRepository positionRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AuditService auditService;

    @Transactional
    public void deleteAccount(Long investorId) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new IllegalArgumentException("Investor not found"));

        boolean hasPositions = positionRepository.findByInvestorId(investorId).stream()
                .anyMatch(p -> p.getCurrentQuantity() > 0);
        if (hasPositions) {
            throw new CannotDeleteAccountException(
                    "No puedes eliminar tu cuenta mientras tengas acciones en tu portafolio. Vende todas tus posiciones primero.");
        }

        boolean hasBalance = accountBalanceRepository.findByInvestorId(investorId)
                .map(b -> b.getTotalBalance().compareTo(BigDecimal.ZERO) > 0)
                .orElse(false);
        if (hasBalance) {
            throw new CannotDeleteAccountException(
                    "No puedes eliminar tu cuenta mientras tengas saldo disponible. Tu balance debe ser $0.");
        }

        investor.setAccountStatus(AccountStatus.DELETED);
        investorRepository.save(investor);

        auditService.record(AuditEventRecord.builder()
                .eventType(AuditEventType.PROFILE_UPDATED)
                .investorId(investorId)
                .performedBy(investorId)
                .result(AuditResult.SUCCESS)
                .detail("account soft-deleted")
                .build());
    }
}
