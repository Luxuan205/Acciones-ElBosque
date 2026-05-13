package com.accioneselbosque.auth.service;

import com.accioneselbosque.auth.dto.*;
import com.accioneselbosque.auth.model.*;
import com.accioneselbosque.auth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final InvestorRepository investorRepository;
    private final InvestorPreferencesRepository preferencesRepository;
    private final ProfileChangeLogRepository changeLogRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long investorId) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new IllegalArgumentException("Investor not found"));
        return mapToProfileResponse(investor);
    }

    @Transactional
    public ProfileResponse updatePersonalData(Long investorId, UpdatePersonalDataRequest request) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new IllegalArgumentException("Investor not found"));

        if (request.fullName() != null && !request.fullName().equals(investor.getFullName())) {
            logChange(investorId, "fullName", investor.getFullName(), request.fullName());
            investor.setFullName(request.fullName());
        }
        if (request.phone() != null && !request.phone().equals(investor.getPhone())) {
            logChange(investorId, "phone", investor.getPhone(), request.phone());
            investor.setPhone(request.phone());
        }

        investorRepository.save(investor);
        return mapToProfileResponse(investor);
    }

    @Transactional
    public PreferencesResponse getOrCreatePreferences(Long investorId) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new IllegalArgumentException("Investor not found"));

        InvestorPreferences prefs = preferencesRepository.findByInvestor(investor)
                .orElseGet(() -> {
                    InvestorPreferences newPrefs = new InvestorPreferences();
                    newPrefs.setInvestor(investor);
                    return preferencesRepository.save(newPrefs);
                });

        return mapToPreferencesResponse(prefs);
    }

    @Transactional
    public PreferencesResponse updatePreferences(Long investorId, UpdatePreferencesRequest request) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new IllegalArgumentException("Investor not found"));

        InvestorPreferences prefs = preferencesRepository.findByInvestor(investor)
                .orElseGet(() -> {
                    InvestorPreferences newPrefs = new InvestorPreferences();
                    newPrefs.setInvestor(investor);
                    return newPrefs;
                });

        if (request.notifChannel() != null) prefs.setNotifChannel(request.notifChannel());
        if (request.language() != null) prefs.setLanguage(request.language());
        prefs.setUpdatedAt(LocalDateTime.now());

        preferencesRepository.save(prefs);
        return mapToPreferencesResponse(prefs);
    }

    private void logChange(Long investorId, String fieldName, String oldValue, String newValue) {
        ProfileChangeLog log = new ProfileChangeLog();
        log.setInvestorId(investorId);
        log.setFieldName(fieldName);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        changeLogRepository.save(log);
    }

    private ProfileResponse mapToProfileResponse(Investor investor) {
        return new ProfileResponse(
                investor.getId(),
                investor.getFullName(),
                investor.getEmail(),
                investor.getDocumentNumber(),
                investor.getPhone(),
                investor.getAccountStatus(),
                investor.getCreatedAt()
        );
    }

    private PreferencesResponse mapToPreferencesResponse(InvestorPreferences prefs) {
        return new PreferencesResponse(
                prefs.getNotifChannel(),
                prefs.getLanguage(),
                prefs.getUpdatedAt()
        );
    }
}
