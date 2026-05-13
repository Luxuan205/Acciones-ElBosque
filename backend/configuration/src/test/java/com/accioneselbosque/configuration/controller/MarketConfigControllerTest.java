package com.accioneselbosque.configuration.controller;

import com.accioneselbosque.auth.config.SecurityConfig;
import com.accioneselbosque.configuration.config.TestJwtConfig;
import com.accioneselbosque.configuration.dto.MarketHolidayDto;
import com.accioneselbosque.configuration.dto.MarketScheduleDto;
import com.accioneselbosque.configuration.dto.MarketStatusDto;
import com.accioneselbosque.configuration.service.MarketScheduleService;
import com.accioneselbosque.configuration.service.MarketStatusService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarketConfigController.class)
@Import({SecurityConfig.class, TestJwtConfig.class})
@TestPropertySource(properties = "app.jwt.secret=TestSecretThatIsAtLeast256BitsLong1234567890ABCDEF")
class MarketConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MarketStatusService marketStatusService;

    @MockitoBean
    private MarketScheduleService marketScheduleService;

    // ── US1: GET /config/market/status ──────────────────────────────────────

    @Test
    void getStatus_authenticated_returns200WithStatus() throws Exception {
        MarketStatusDto dto = MarketStatusDto.builder()
            .status("OPEN")
            .today(LocalDate.of(2026, 5, 10))
            .currentTime("11:30:00")
            .timezone("America/Bogota")
            .nextClose("15:30:00")
            .build();
        when(marketStatusService.getStatus()).thenReturn(dto);

        mockMvc.perform(get("/config/market/status").with(user("investor").roles("INVESTOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.timezone").value("America/Bogota"))
            .andExpect(jsonPath("$.nextClose").value("15:30:00"));
    }

    @Test
    void getStatus_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/config/market/status"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getStatus_marketClosed_returnsClosedStatus() throws Exception {
        MarketStatusDto dto = MarketStatusDto.builder()
            .status("CLOSED")
            .today(LocalDate.of(2026, 5, 10))
            .currentTime("18:00:00")
            .timezone("America/Bogota")
            .nextOpen("09:00:00")
            .build();
        when(marketStatusService.getStatus()).thenReturn(dto);

        mockMvc.perform(get("/config/market/status").with(user("investor").roles("INVESTOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"))
            .andExpect(jsonPath("$.nextOpen").value("09:00:00"));
    }

    // ── US1: PUT /config/market/schedule ────────────────────────────────────

    @Test
    void updateSchedule_asAdmin_returns200() throws Exception {
        String adminId = "00000000-0000-0000-0000-000000000001";
        MarketScheduleDto request = MarketScheduleDto.builder()
            .openTime("09:30")
            .closeTime("16:00")
            .workingDays(List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"))
            .build();
        MarketScheduleDto response = MarketScheduleDto.builder()
            .openTime("09:30").closeTime("16:00")
            .workingDays(List.of("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"))
            .timezone("America/Bogota")
            .build();
        when(marketScheduleService.updateSchedule(any(), any())).thenReturn(response);

        mockMvc.perform(put("/config/market/schedule")
                .with(user(adminId).roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openTime").value("09:30"));
    }

    @Test
    void updateSchedule_asInvestor_returns403() throws Exception {
        MarketScheduleDto request = MarketScheduleDto.builder()
            .openTime("09:00").closeTime("15:30")
            .workingDays(List.of("MONDAY"))
            .build();

        mockMvc.perform(put("/config/market/schedule")
                .with(user("investor").roles("INVESTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateSchedule_missingOpenTime_returns400() throws Exception {
        mockMvc.perform(put("/config/market/schedule")
                .with(user("00000000-0000-0000-0000-000000000001").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"closeTime\":\"15:30\",\"workingDays\":[\"MONDAY\"]}"))
            .andExpect(status().isBadRequest());
    }

    // ── US2: POST /config/market/holidays ───────────────────────────────────

    @Test
    void addHoliday_valid_returns201() throws Exception {
        MarketHolidayDto request = MarketHolidayDto.builder()
            .date(LocalDate.of(2026, 8, 7))
            .description("Batalla de Boyacá")
            .type("NATIONAL")
            .build();
        MarketHolidayDto saved = MarketHolidayDto.builder()
            .id(UUID.randomUUID())
            .date(LocalDate.of(2026, 8, 7))
            .description("Batalla de Boyacá")
            .type("NATIONAL")
            .build();
        when(marketScheduleService.addHoliday(any())).thenReturn(saved);

        mockMvc.perform(post("/config/market/holidays")
                .with(user("00000000-0000-0000-0000-000000000001").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.date").value("2026-08-07"));
    }

    @Test
    void addHoliday_asInvestor_returns403() throws Exception {
        MarketHolidayDto request = MarketHolidayDto.builder()
            .date(LocalDate.of(2026, 8, 7))
            .description("Test")
            .type("NATIONAL")
            .build();

        mockMvc.perform(post("/config/market/holidays")
                .with(user("investor").roles("INVESTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void addHoliday_invalidType_returns400() throws Exception {
        mockMvc.perform(post("/config/market/holidays")
                .with(user("00000000-0000-0000-0000-000000000001").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-08-07\",\"description\":\"Test\",\"type\":\"INVALID\"}"))
            .andExpect(status().isBadRequest());
    }

    // ── US2: DELETE /config/market/holidays/{id} ────────────────────────────

    @Test
    void deleteHoliday_existing_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(marketScheduleService).deleteHoliday(id);

        mockMvc.perform(delete("/config/market/holidays/" + id)
                .with(user("00000000-0000-0000-0000-000000000001").roles("ADMIN")))
            .andExpect(status().isNoContent());
    }

    // ── US2: GET /config/market/holidays ────────────────────────────────────

    @Test
    void listHolidays_returns200WithList() throws Exception {
        MarketHolidayDto holiday = MarketHolidayDto.builder()
            .id(UUID.randomUUID())
            .date(LocalDate.of(2026, 5, 1))
            .description("Día del Trabajo")
            .type("NATIONAL")
            .build();
        when(marketScheduleService.listHolidays(anyInt())).thenReturn(List.of(holiday));

        mockMvc.perform(get("/config/market/holidays")
                .with(user("00000000-0000-0000-0000-000000000001").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.holidays[0].description").value("Día del Trabajo"));
    }
}
