package com.accioneselbosque.auth.service;

import com.accioneselbosque.auth.dto.AdminUserDetailDto;
import com.accioneselbosque.auth.dto.AdminUserDto;
import com.accioneselbosque.auth.dto.PagedUsersResponse;
import com.accioneselbosque.auth.dto.RecentActivityDto;
import com.accioneselbosque.auth.dto.UpdateUserRoleRequest;
import com.accioneselbosque.auth.dto.UpdateUserStatusRequest;
import com.accioneselbosque.auth.exception.AdminConfirmationRequiredException;
import com.accioneselbosque.auth.exception.LastAdminException;
import com.accioneselbosque.auth.exception.UserNotFoundException;
import com.accioneselbosque.auth.model.AccountStatus;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.model.InvestorRole;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.auth.repository.InvestorSpecification;
import com.accioneselbosque.auth.repository.MfaSessionRepository;
import com.accioneselbosque.audit.model.AuditEventRecord;
import com.accioneselbosque.audit.model.AuditEventType;
import com.accioneselbosque.audit.model.AuditResult;
import com.accioneselbosque.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final InvestorRepository investorRepository;
    private final MfaSessionRepository mfaSessionRepository;
    private final VerificationTokenService verificationTokenService;
    private final MailService mailService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedUsersResponse searchUsers(String email, String name, String status,
                                           String subscriptionType, int page, int size) {
        Specification<Investor> spec = (root, query, cb) -> cb.conjunction();
        if (email != null && !email.isBlank()) {
            spec = spec.and(InvestorSpecification.hasEmailContaining(email));
        }
        if (name != null && !name.isBlank()) {
            spec = spec.and(InvestorSpecification.hasNameContaining(name));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and(InvestorSpecification.hasStatus(status));
        }
        if (subscriptionType != null && !subscriptionType.isBlank()) {
            spec = spec.and(InvestorSpecification.hasSubscriptionType(subscriptionType));
        }

        Page<Investor> resultPage = investorRepository.findAll(
                spec, PageRequest.of(page, size, Sort.by("createdAt").descending()));

        return new PagedUsersResponse(
                resultPage.getContent().stream().map(this::toAdminUserDto).toList(),
                resultPage.getTotalElements(),
                resultPage.getNumber(),
                resultPage.getSize()
        );
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDto getUserDetail(Long userId) {
        Investor investor = investorRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return new AdminUserDetailDto(
                investor.getId(),
                investor.getFullName(),
                investor.getEmail(),
                investor.getDocumentNumber(),
                investor.getAccountStatus().name(),
                investor.getSubscriptionType().name(),
                investor.getSubscriptionExpiresAt(),
                roleOf(investor),
                investor.getCreatedAt(),
                List.of()
        );
    }

    @Transactional
    public AdminUserDto updateUserStatus(Long adminId, Long userId, UpdateUserStatusRequest req) {
        Investor investor = investorRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        AccountStatus newStatus = AccountStatus.valueOf(req.newStatus());

        if (newStatus == AccountStatus.SUSPENDED && investor.getRole() == InvestorRole.ADMIN) {
            long activeAdmins = investorRepository.countByRoleAndAccountStatus(
                    InvestorRole.ADMIN, AccountStatus.ACTIVE);
            if (activeAdmins <= 1) {
                throw new LastAdminException();
            }
        }

        AccountStatus prevStatus = investor.getAccountStatus();
        investor.setAccountStatus(newStatus);
        if (newStatus == AccountStatus.SUSPENDED) {
            mfaSessionRepository.deleteByInvestorId(userId);
        }

        investorRepository.save(investor);

        AuditEventType eventType = newStatus == AccountStatus.SUSPENDED
                ? AuditEventType.USER_SUSPENDED
                : (prevStatus == AccountStatus.SUSPENDED ? AuditEventType.USER_UNSUSPENDED : null);
        if (eventType != null) {
            auditService.record(AuditEventRecord.builder()
                    .eventType(eventType)
                    .investorId(userId)
                    .performedBy(adminId)
                    .result(AuditResult.SUCCESS)
                    .detail(prevStatus + " -> " + newStatus + " (reason: " + req.reason() + ")")
                    .build());
        }
        return toAdminUserDto(investor);
    }

    @Transactional
    public AdminUserDto updateUserRole(Long adminId, Long userId, UpdateUserRoleRequest req) {
        Investor investor = investorRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (req.newRole() == InvestorRole.ADMIN && !req.confirmed()) {
            throw new AdminConfirmationRequiredException();
        }

        if (investor.getRole() == InvestorRole.ADMIN) {
            long activeAdmins = investorRepository.countByRoleAndAccountStatus(
                    InvestorRole.ADMIN, AccountStatus.ACTIVE);
            if (activeAdmins <= 1) {
                throw new LastAdminException();
            }
        }

        if (investor.getRole() == InvestorRole.ADMIN && req.newRole() != InvestorRole.ADMIN) {
            mfaSessionRepository.deleteByInvestorId(userId);
        }

        InvestorRole prevRole = investor.getRole();
        investor.setRole(req.newRole());
        investorRepository.save(investor);
        auditService.record(AuditEventRecord.builder()
                .eventType(AuditEventType.ROLE_CHANGED)
                .investorId(userId)
                .performedBy(adminId)
                .result(AuditResult.SUCCESS)
                .detail(prevRole + " -> " + req.newRole() + " (reason: " + req.reason() + ")")
                .build());
        return toAdminUserDto(investor);
    }

    @Transactional
    public void initiatePasswordReset(Long adminId, Long userId) {
        Investor investor = investorRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        var token = verificationTokenService.createToken(investor);
        mailService.sendPasswordResetEmail(investor.getEmail(), token.getToken());
        auditService.record(AuditEventRecord.builder()
                .eventType(AuditEventType.ADMIN_INITIATED_PASSWORD_RESET)
                .investorId(userId)
                .performedBy(adminId)
                .result(AuditResult.SUCCESS)
                .build());
    }

    private AdminUserDto toAdminUserDto(Investor investor) {
        return new AdminUserDto(
                investor.getId(),
                investor.getFullName(),
                investor.getEmail(),
                investor.getDocumentNumber(),
                investor.getAccountStatus().name(),
                investor.getSubscriptionType().name(),
                investor.getSubscriptionExpiresAt(),
                roleOf(investor),
                investor.getCreatedAt()
        );
    }

    private String roleOf(Investor investor) {
        return investor.getRole() != null ? investor.getRole().name() : InvestorRole.INVESTOR.name();
    }
}
