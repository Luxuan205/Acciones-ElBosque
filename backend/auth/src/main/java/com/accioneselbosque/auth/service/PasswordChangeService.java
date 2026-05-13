package com.accioneselbosque.auth.service;

import com.accioneselbosque.auth.dto.ChangePasswordRequest;
import com.accioneselbosque.auth.exception.InvalidCurrentPasswordException;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.model.ProfileChangeLog;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.auth.repository.ProfileChangeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordChangeService {

    private final InvestorRepository investorRepository;
    private final ProfileChangeLogRepository changeLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changePassword(Long investorId, ChangePasswordRequest request) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new IllegalArgumentException("Investor not found"));

        if (!passwordEncoder.matches(request.currentPassword(), investor.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        investor.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        investorRepository.save(investor);

        ProfileChangeLog log = new ProfileChangeLog();
        log.setInvestorId(investorId);
        log.setFieldName("password");
        log.setOldValue("[REDACTED]");
        log.setNewValue("[REDACTED]");
        changeLogRepository.save(log);
    }
}
