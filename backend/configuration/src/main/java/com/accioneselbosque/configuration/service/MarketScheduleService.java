package com.accioneselbosque.configuration.service;

import com.accioneselbosque.configuration.dto.MarketHolidayDto;
import com.accioneselbosque.configuration.dto.MarketScheduleDto;
import com.accioneselbosque.configuration.exception.HolidayAlreadyExistsException;
import com.accioneselbosque.configuration.exception.HolidayNotFoundException;
import com.accioneselbosque.configuration.exception.InvalidScheduleException;
import com.accioneselbosque.configuration.model.MarketHoliday;
import com.accioneselbosque.configuration.model.MarketSchedule;
import com.accioneselbosque.configuration.repository.MarketHolidayRepository;
import com.accioneselbosque.configuration.repository.MarketScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketScheduleService {

    private final MarketScheduleRepository scheduleRepository;
    private final MarketHolidayRepository holidayRepository;
    private final MarketStatusService statusService;

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public MarketScheduleDto getSchedule() {
        MarketSchedule s = scheduleRepository.findFirst()
            .orElseThrow(() -> new IllegalStateException("No market schedule configured"));
        return toScheduleDto(s);
    }

    @Transactional
    public MarketScheduleDto updateSchedule(MarketScheduleDto dto, UUID adminId) {
        LocalTime openTime = LocalTime.parse(dto.getOpenTime(), HH_MM);
        LocalTime closeTime = LocalTime.parse(dto.getCloseTime(), HH_MM);
        if (!openTime.isBefore(closeTime)) {
            throw new InvalidScheduleException("openTime must be before closeTime");
        }

        MarketSchedule schedule = scheduleRepository.findFirst().orElse(new MarketSchedule());
        schedule.setOpenTime(openTime);
        schedule.setCloseTime(closeTime);
        schedule.setWorkingDays(dto.getWorkingDays().stream()
            .map(DayOfWeek::valueOf)
            .collect(Collectors.toSet()));
        schedule.setUpdatedBy(adminId);

        MarketSchedule saved = scheduleRepository.save(schedule);
        statusService.refreshStatus();
        return toScheduleDto(saved);
    }

    public List<MarketHolidayDto> listHolidays(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return holidayRepository.findByDateBetweenOrderByDateAsc(start, end).stream()
            .map(this::toHolidayDto)
            .toList();
    }

    @Transactional
    public MarketHolidayDto addHoliday(MarketHolidayDto dto) {
        if (holidayRepository.existsByDate(dto.getDate())) {
            throw new HolidayAlreadyExistsException(dto.getDate());
        }
        MarketHoliday holiday = new MarketHoliday();
        holiday.setDate(dto.getDate());
        holiday.setDescription(dto.getDescription());
        holiday.setType(dto.getType());
        return toHolidayDto(holidayRepository.save(holiday));
    }

    @Transactional
    public void deleteHoliday(UUID id) {
        if (!holidayRepository.existsById(id)) {
            throw new HolidayNotFoundException();
        }
        holidayRepository.deleteById(id);
    }

    private MarketScheduleDto toScheduleDto(MarketSchedule s) {
        return MarketScheduleDto.builder()
            .openTime(s.getOpenTime().format(HH_MM))
            .closeTime(s.getCloseTime().format(HH_MM))
            .workingDays(s.getWorkingDays().stream().map(DayOfWeek::name).sorted().toList())
            .timezone("America/Bogota")
            .updatedAt(s.getUpdatedAt())
            .build();
    }

    private MarketHolidayDto toHolidayDto(MarketHoliday h) {
        return MarketHolidayDto.builder()
            .id(h.getId())
            .date(h.getDate())
            .description(h.getDescription())
            .type(h.getType())
            .createdAt(h.getCreatedAt())
            .build();
    }
}
