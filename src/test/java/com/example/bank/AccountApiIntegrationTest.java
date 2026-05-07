package com.example.bank;

import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.BankOperationRepository;
import com.example.bank.repository.LedgerEntryRepository;
import com.example.bank.service.ExternalLoggingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:api-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AccountApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BankOperationRepository bankOperationRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @MockBean
    private ExternalLoggingService externalLoggingService;

    @BeforeEach
    void cleanDatabase() {
        ledgerEntryRepository.deleteAll();
        bankOperationRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void fullAccountFlowKeepsBalancesSeparatedByCurrency() throws Exception {
        String accountNumber = createAccount("alice");

        mockMvc.perform(post("/accounts/{accountNumber}/credit", accountNumber)
                        .header("X-Username", "alice")
                        .contentType("application/json")
                        .content("""
                                {"amount":100.00,"currency":"EUR","referenceId":"11111111-1111-1111-1111-111111111111"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/accounts/{accountNumber}/debit", accountNumber)
                        .header("X-Username", "alice")
                        .contentType("application/json")
                        .content("""
                                {"amount":30.00,"currency":"EUR","referenceId":"22222222-2222-2222-2222-222222222222"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/accounts/{accountNumber}/exchange", accountNumber)
                        .header("X-Username", "alice")
                        .contentType("application/json")
                        .content("""
                                {"amount":20.00,"fromCurrency":"EUR","toCurrency":"USD","referenceId":"33333333-3333-3333-3333-333333333333"}
                                """))
                .andExpect(status().isNoContent());

        Map<String, BigDecimal> balances = getBalances(accountNumber);
        assertThat(balances.get("EUR")).isEqualByComparingTo("50.00");
        assertThat(balances.get("USD")).isEqualByComparingTo("21.60");
        verify(externalLoggingService, times(1)).logDebit();
    }

    @Test
    void duplicateReferenceDoesNotApplyOperationTwice() throws Exception {
        String accountNumber = createAccount("alice");
        String credit = """
                {"amount":100.00,"currency":"EUR","referenceId":"44444444-4444-4444-4444-444444444444"}
                """;

        mockMvc.perform(post("/accounts/{accountNumber}/credit", accountNumber)
                        .header("X-Username", "alice")
                        .contentType("application/json")
                        .content(credit))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/accounts/{accountNumber}/credit", accountNumber)
                        .header("X-Username", "alice")
                        .contentType("application/json")
                        .content(credit))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Duplicate reference ID"));

        assertThat(getBalances(accountNumber).get("EUR")).isEqualByComparingTo("100.00");
        assertThat(ledgerEntryRepository.count()).isEqualTo(1);
        assertThat(bankOperationRepository.count()).isEqualTo(1);
    }

    @Test
    void duplicateDebitReferenceDoesNotCallExternalLoggingAgain() throws Exception {
        String accountNumber = createAccount("alice");

        mockMvc.perform(post("/accounts/{accountNumber}/credit", accountNumber)
                        .header("X-Username", "alice")
                        .contentType("application/json")
                        .content("""
                                {"amount":100.00,"currency":"EUR","referenceId":"88888888-8888-8888-8888-888888888888"}
                                """))
                .andExpect(status().isNoContent());

        String debit = """
                {"amount":10.00,"currency":"EUR","referenceId":"99999999-9999-9999-9999-999999999999"}
                """;

        mockMvc.perform(post("/accounts/{accountNumber}/debit", accountNumber)
                        .header("X-Username", "alice")
                        .contentType("application/json")
                        .content(debit))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/accounts/{accountNumber}/debit", accountNumber)
                        .header("X-Username", "alice")
                        .contentType("application/json")
                        .content(debit))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Duplicate reference ID"));

        assertThat(getBalances(accountNumber).get("EUR")).isEqualByComparingTo("90.00");
        verify(externalLoggingService, times(1)).logDebit();
    }

    @Test
    void debitUsesOnlyRequestedCurrency() throws Exception {
        String accountNumber = createAccount("alice");

        mockMvc.perform(post("/accounts/{accountNumber}/credit", accountNumber)
                        .header("X-Username", "alice")
                        .contentType("application/json")
                        .content("""
                                {"amount":100.00,"currency":"USD","referenceId":"55555555-5555-5555-5555-555555555555"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/accounts/{accountNumber}/debit", accountNumber)
                        .header("X-Username", "alice")
                        .contentType("application/json")
                        .content("""
                                {"amount":1.00,"currency":"EUR","referenceId":"66666666-6666-6666-6666-666666666666"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Insufficient funds"));

        Map<String, BigDecimal> balances = getBalances(accountNumber);
        assertThat(balances.get("USD")).isEqualByComparingTo("100.00");
        assertThat(balances).doesNotContainKey("EUR");
    }

    @Test
    void unsupportedCurrencyReturnsBadRequest() throws Exception {
        String accountNumber = createAccount("alice");

        mockMvc.perform(post("/accounts/{accountNumber}/credit", accountNumber)
                        .header("X-Username", "alice")
                        .contentType("application/json")
                        .content("""
                                {"amount":10.00,"currency":"JPY","referenceId":"77777777-7777-7777-7777-777777777777"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unsupported currency: JPY"));

        verifyNoInteractions(externalLoggingService);
    }

    private String createAccount(String username) throws Exception {
        String body = mockMvc.perform(post("/accounts")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(body);
        return jsonNode.get("accountNumber").asText();
    }

    private Map<String, BigDecimal> getBalances(String accountNumber) throws Exception {
        String body = mockMvc.perform(get("/accounts/{accountNumber}/balance", accountNumber)
                        .header("X-Username", "alice"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(body, new TypeReference<>() {
        });
    }
}
