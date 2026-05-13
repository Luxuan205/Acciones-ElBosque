package com.accioneselbosque.configuration.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Converter(autoApply = true)
public class WorkingDaysConverter implements AttributeConverter<Set<DayOfWeek>, Integer> {

    private static final Map<DayOfWeek, Integer> DAY_BITS = Map.of(
        DayOfWeek.MONDAY,    1,
        DayOfWeek.TUESDAY,   2,
        DayOfWeek.WEDNESDAY, 4,
        DayOfWeek.THURSDAY,  8,
        DayOfWeek.FRIDAY,   16,
        DayOfWeek.SATURDAY, 32,
        DayOfWeek.SUNDAY,   64
    );

    @Override
    public Integer convertToDatabaseColumn(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) return 0;
        return days.stream().mapToInt(d -> DAY_BITS.getOrDefault(d, 0)).sum();
    }

    @Override
    public Set<DayOfWeek> convertToEntityAttribute(Integer value) {
        if (value == null || value == 0) return EnumSet.noneOf(DayOfWeek.class);
        return DAY_BITS.entrySet().stream()
            .filter(e -> (value & e.getValue()) != 0)
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }
}
