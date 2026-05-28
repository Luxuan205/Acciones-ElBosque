package com.accioneselbosque.app.service;

import com.accioneselbosque.app.dto.AdminTransactionDto;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.configuration.service.MarketStatusService;
import com.accioneselbosque.orders.repository.OrderRepository;
import com.accioneselbosque.portfolio.model.Transaction;
import com.accioneselbosque.portfolio.model.TransactionType;
import com.accioneselbosque.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceAdminTransactionsTest {

    @Mock private MarketStatusService marketStatusService;
    @Mock private OrderRepository orderRepository;
    @Mock private InvestorRepository investorRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private DashboardService dashboardService;

    @Test
    void getAdminTransactions_retornaPageConDatosDelInversor() {
        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setInvestorId(10L);
        tx.setSymbol("ECOPETROL");
        tx.setTransactionType(TransactionType.BUY);
        tx.setQuantity(5);
        tx.setGrossAmount(new BigDecimal("500000"));
        tx.setCommission(new BigDecimal("2500"));
        tx.setNetAmount(new BigDecimal("497500"));
        tx.setExecutedAt(LocalDateTime.now());

        Investor investor = new Investor();
        investor.setId(10L);
        investor.setFullName("Juan Pérez");
        investor.setEmail("juan@test.com");

        when(transactionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));
        when(investorRepository.findAllById(any()))
                .thenReturn(List.of(investor));

        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "executedAt"));

        Page<AdminTransactionDto> result = dashboardService.getAdminTransactions(
                null, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        AdminTransactionDto dto = result.getContent().get(0);
        assertThat(dto.investorName()).isEqualTo("Juan Pérez");
        assertThat(dto.investorEmail()).isEqualTo("juan@test.com");
        assertThat(dto.symbol()).isEqualTo("ECOPETROL");
        assertThat(dto.type()).isEqualTo("BUY");
        assertThat(dto.quantity()).isEqualTo(5);
        assertThat(dto.grossAmount()).isEqualByComparingTo("500000");
        assertThat(dto.commission()).isEqualByComparingTo("2500");
    }

    @Test
    void getAdminTransactions_inversoresSinRegistroMuestraDash() {
        Transaction tx = new Transaction();
        tx.setId(2L);
        tx.setInvestorId(99L);
        tx.setSymbol("ISA");
        tx.setTransactionType(TransactionType.SELL);
        tx.setQuantity(1);
        tx.setGrossAmount(new BigDecimal("100000"));
        tx.setCommission(new BigDecimal("500"));
        tx.setNetAmount(new BigDecimal("99500"));
        tx.setExecutedAt(LocalDateTime.now());

        when(transactionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));
        when(investorRepository.findAllById(any())).thenReturn(List.of());

        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "executedAt"));

        Page<AdminTransactionDto> result = dashboardService.getAdminTransactions(
                null, null, null, null, null, pageable);

        assertThat(result.getContent().get(0).investorName()).isEqualTo("—");
        assertThat(result.getContent().get(0).investorEmail()).isEqualTo("—");
    }

    @Test
    void getAdminTransactions_paginaVaciaRetornaContentVacio() {
        when(transactionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(investorRepository.findAllById(any())).thenReturn(List.of());

        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "executedAt"));

        Page<AdminTransactionDto> result = dashboardService.getAdminTransactions(
                null, null, null, null, null, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
