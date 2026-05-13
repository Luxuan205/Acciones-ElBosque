package com.accioneselbosque.configuration.service;

import com.accioneselbosque.configuration.dto.MarketStatusDto;
import com.accioneselbosque.configuration.model.MarketHoliday;
import com.accioneselbosque.configuration.model.MarketSchedule;
import com.accioneselbosque.configuration.repository.MarketHolidayRepository;
import com.accioneselbosque.configuration.repository.MarketScheduleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketStatusService {

    private final MarketScheduleRepository scheduleRepository;
    private final MarketHolidayRepository holidayRepository;

    private final AtomicBoolean marketOpen = new AtomicBoolean(false);
    private volatile MarketStatusDto currentStatus;

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @PostConstruct
    public void init() {
        refreshStatus();
    }

    @Scheduled(fixedRate = 60_000)
    public void refreshStatus() {
        Optional<MarketSchedule> scheduleOpt = scheduleRepository.findFirst();
        if (scheduleOpt.isEmpty()) {
            marketOpen.set(false);
            currentStatus = buildStatus(false, false, null, null, null);
            return;
        }

        MarketSchedule schedule = scheduleOpt.get();
        ZonedDateTime now = ZonedDateTime.now(BOGOTA);
        LocalDate today = now.toLocalDate();
        LocalTime time = now.toLocalTime();

        Optional<MarketHoliday> holiday = holidayRepository.findByDate(today);
        if (holiday.isPresent()) {
            marketOpen.set(false);
            currentStatus = buildStatus(false, true, holiday.get().getDescription(), null,
                schedule.getOpenTime().format(TIME_FMT));
            return;
        }

        DayOfWeek dow = today.getDayOfWeek();
        if (!schedule.getWorkingDays().contains(dow)) {
            marketOpen.set(false);
            currentStatus = buildStatus(false, false, null, null,
                schedule.getOpenTime().format(TIME_FMT));
            return;
        }

        boolean open = !time.isBefore(schedule.getOpenTime()) && time.isBefore(schedule.getCloseTime());
        marketOpen.set(open);

        currentStatus = MarketStatusDto.builder()
            .status(open ? "OPEN" : "CLOSED")
            .today(today)
            .currentTime(time.format(TIME_FMT))
            .timezone("America/Bogota")
            .nextClose(open ? schedule.getCloseTime().format(TIME_FMT) : null)
            .nextOpen(open ? null : schedule.getOpenTime().format(TIME_FMT))
            .holiday(false)
            .holidayName(null)
            .build();

        log.debug("Market status refreshed: {}", open ? "OPEN" : "CLOSED");
    }

    public boolean isMarketOpen() {
        return marketOpen.get();
    }

    public MarketStatusDto getStatus() {
        if (currentStatus == null) {
            refreshStatus();
        }
        return currentStatus;
    }

    private MarketStatusDto buildStatus(boolean open, boolean isHoliday,
                                         String holidayName, String nextClose, String nextOpen) {
        ZonedDateTime now = ZonedDateTime.now(BOGOTA);
        return MarketStatusDto.builder()
            .status(open ? "OPEN" : "CLOSED")
            .today(now.toLocalDate())
            .currentTime(now.toLocalTime().format(TIME_FMT))
            .timezone("America/Bogota")
            .nextClose(nextClose)
            .nextOpen(nextOpen)
            .holiday(isHoliday)
            .holidayName(holidayName)
            .build();
    }
}
