package com.accioneselbosque.configuration.service;

import com.accioneselbosque.configuration.dto.GlobalParameterDto;
import com.accioneselbosque.configuration.dto.GroupedParametersResponse;
import com.accioneselbosque.configuration.dto.ParameterChangeHistoryDto;
import com.accioneselbosque.configuration.dto.UpdateParameterRequest;
import com.accioneselbosque.configuration.exception.InvalidParameterValueException;
import com.accioneselbosque.configuration.exception.ParameterNotFoundException;
import com.accioneselbosque.configuration.model.GlobalParameter;
import com.accioneselbosque.configuration.model.ParameterChangeHistory;
import com.accioneselbosque.configuration.model.ParameterDataType;
import com.accioneselbosque.configuration.repository.GlobalParameterRepository;
import com.accioneselbosque.configuration.repository.ParameterChangeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobalParameterService {

    private final GlobalParameterRepository globalParameterRepository;
    private final ParameterChangeHistoryRepository changeHistoryRepository;

    @Cacheable("parameters")
    @Transactional(readOnly = true)
    public GroupedParametersResponse getAllGroupedByCategory() {
        Map<String, List<GlobalParameterDto>> grouped = globalParameterRepository
                .findAllByOrderByCategoryAscKeyAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.groupingBy(GlobalParameterDto::category));
        return new GroupedParametersResponse(grouped);
    }

    @CacheEvict(value = "parameters", allEntries = true)
    @Transactional
    public GlobalParameterDto updateParameter(String key, UpdateParameterRequest req, Long adminId) {
        GlobalParameter param = globalParameterRepository.findById(key)
                .orElseThrow(() -> new ParameterNotFoundException(key));

        validateValue(param, req.value());

        String previousValue = param.getValue();
        param.setValue(req.value());
        globalParameterRepository.save(param);

        changeHistoryRepository.save(ParameterChangeHistory.builder()
                .parameterKey(key)
                .previousValue(previousValue)
                .newValue(req.value())
                .changedBy(adminId)
                .changedAt(LocalDateTime.now())
                .reason(req.reason())
                .build());

        return toDto(param);
    }

    @Transactional(readOnly = true)
    public List<ParameterChangeHistoryDto> getHistory(String key) {
        if (!globalParameterRepository.existsById(key)) {
            throw new ParameterNotFoundException(key);
        }
        return changeHistoryRepository.findByParameterKeyOrderByChangedAtDesc(key)
                .stream()
                .map(this::toHistoryDto)
                .toList();
    }

    @CacheEvict(value = "parameters", allEntries = true)
    @Transactional
    public GlobalParameterDto revertParameter(String key, Long adminId) {
        ParameterChangeHistory latest = changeHistoryRepository
                .findTopByParameterKeyOrderByChangedAtDesc(key)
                .orElseThrow(() -> new IllegalStateException("No change history found for parameter: " + key));

        UpdateParameterRequest revertReq = new UpdateParameterRequest(latest.getPreviousValue(), "Revert");
        return updateParameter(key, revertReq, adminId);
    }

    private void validateValue(GlobalParameter param, String newValue) {
        if (param.getDataType() == ParameterDataType.INTEGER) {
            try {
                int val = Integer.parseInt(newValue);
                if (param.getMinValue() != null && val < Integer.parseInt(param.getMinValue())) {
                    throw new InvalidParameterValueException(param.getKey(),
                            "Value " + val + " is below minimum " + param.getMinValue());
                }
                if (param.getMaxValue() != null && val > Integer.parseInt(param.getMaxValue())) {
                    throw new InvalidParameterValueException(param.getKey(),
                            "Value " + val + " exceeds maximum " + param.getMaxValue());
                }
            } catch (NumberFormatException e) {
                throw new InvalidParameterValueException(param.getKey(), "Must be a valid integer");
            }
        } else if (param.getDataType() == ParameterDataType.DECIMAL) {
            try {
                BigDecimal val = new BigDecimal(newValue);
                if (param.getMinValue() != null && val.compareTo(new BigDecimal(param.getMinValue())) < 0) {
                    throw new InvalidParameterValueException(param.getKey(),
                            "Value is below minimum " + param.getMinValue());
                }
                if (param.getMaxValue() != null && val.compareTo(new BigDecimal(param.getMaxValue())) > 0) {
                    throw new InvalidParameterValueException(param.getKey(),
                            "Value exceeds maximum " + param.getMaxValue());
                }
            } catch (NumberFormatException e) {
                throw new InvalidParameterValueException(param.getKey(), "Must be a valid decimal number");
            }
        } else if (param.getDataType() == ParameterDataType.BOOLEAN) {
            if (!"true".equalsIgnoreCase(newValue) && !"false".equalsIgnoreCase(newValue)) {
                throw new InvalidParameterValueException(param.getKey(), "Must be 'true' or 'false'");
            }
        }
    }

    private GlobalParameterDto toDto(GlobalParameter p) {
        return new GlobalParameterDto(p.getKey(), p.getValue(), p.getDataType(),
                p.getCategory(), p.getDescription(), p.getMinValue(), p.getMaxValue());
    }

    private ParameterChangeHistoryDto toHistoryDto(ParameterChangeHistory h) {
        return new ParameterChangeHistoryDto(h.getId(), h.getParameterKey(),
                h.getPreviousValue(), h.getNewValue(), h.getChangedBy(),
                h.getChangedAt(), h.getReason());
    }
}
