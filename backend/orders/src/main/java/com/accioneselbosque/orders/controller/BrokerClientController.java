package com.accioneselbosque.orders.controller;

import com.accioneselbosque.auth.dto.ClientDetailDto;
import com.accioneselbosque.auth.dto.ClientSummaryDto;
import com.accioneselbosque.orders.service.BrokerClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/brokers/me/clients")
@PreAuthorize("hasRole('BROKER')")
@RequiredArgsConstructor
public class BrokerClientController {

    private final BrokerClientService brokerClientService;

    @GetMapping
    public ResponseEntity<List<ClientSummaryDto>> getClients(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication) {
        Long brokerId = Long.parseLong(authentication.getName());
        List<ClientSummaryDto> clients = brokerClientService.getClients(brokerId, search, status, page);
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{investorId}")
    public ResponseEntity<ClientDetailDto> getClientDetail(
            @PathVariable Long investorId,
            Authentication authentication) {
        Long brokerId = Long.parseLong(authentication.getName());
        ClientDetailDto detail = brokerClientService.getClientDetail(brokerId, investorId);
        return ResponseEntity.ok(detail);
    }
}
