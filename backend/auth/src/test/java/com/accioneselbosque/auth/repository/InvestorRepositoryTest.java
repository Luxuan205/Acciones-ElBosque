package com.accioneselbosque.auth.repository;

import com.accioneselbosque.auth.model.AccountStatus;
import com.accioneselbosque.auth.model.Investor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.properties.hibernate.default_schema="
})
class InvestorRepositoryTest {

    @Autowired
    private InvestorRepository investorRepository;

    private Investor buildInvestor(String email, String documentNumber) {
        Investor investor = new Investor();
        investor.setFullName("Test Investor");
        investor.setDocumentNumber(documentNumber);
        investor.setEmail(email);
        investor.setPasswordHash("$2a$12$hashedpassword123456789012345678901234567890123456789");
        investor.setAccountStatus(AccountStatus.PENDING);
        return investor;
    }

    @Test
    void findByEmail_returnsInvestorWhenExists() {
        Investor saved = investorRepository.save(buildInvestor("test@ejemplo.com", "1234567890"));

        Optional<Investor> found = investorRepository.findByEmail("test@ejemplo.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getEmail()).isEqualTo("test@ejemplo.com");
    }

    @Test
    void findByEmail_returnsEmptyWhenNotExists() {
        Optional<Investor> found = investorRepository.findByEmail("notfound@ejemplo.com");
        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_returnsTrueWhenExists() {
        investorRepository.save(buildInvestor("exists@ejemplo.com", "9876543210"));

        assertThat(investorRepository.existsByEmail("exists@ejemplo.com")).isTrue();
    }

    @Test
    void existsByEmail_returnsFalseWhenNotExists() {
        assertThat(investorRepository.existsByEmail("noexiste@ejemplo.com")).isFalse();
    }

    @Test
    void existsByDocumentNumber_returnsTrueWhenExists() {
        investorRepository.save(buildInvestor("doc@ejemplo.com", "1111111111"));

        assertThat(investorRepository.existsByDocumentNumber("1111111111")).isTrue();
    }
}
