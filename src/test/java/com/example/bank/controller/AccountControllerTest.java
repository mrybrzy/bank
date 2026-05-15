package com.example.bank.controller;

import com.example.bank.dto.AccountResponse;
import com.example.bank.exception.ApplicationException;
import com.example.bank.exception.GlobalExceptionHandler;
import com.example.bank.service.AccountService;
import com.example.bank.service.ExternalLoggingService;
import org.mockito.InOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private ExternalLoggingService externalLoggingService;

    private static final String ACCOUNT_NUMBER = "ACC-12345678";
    private static final String USERNAME = "alice";

    @Test
    void createAccountReturnsCreatedAccountDto() throws Exception {
        when(accountService.createAccount(any())).thenReturn(new AccountResponse(ACCOUNT_NUMBER, USERNAME));

        mockMvc.perform(post("/accounts")
                        .contentType("application/json")
                        .content("""
                                {"username":"alice"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER))
                .andExpect(jsonPath("$.username").value(USERNAME));
    }

    @Test
    void creditRejectsInvalidAmountBeforeCallingService() throws Exception {
        mockMvc.perform(post("/accounts/ACC-12345678/credit")
                        .header("X-Username", USERNAME)
                        .contentType("application/json")
                        .content("""
                                {"amount":0,"currency":"EUR","referenceId":"11111111-1111-1111-1111-111111111111"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount").exists());

        verifyNoInteractions(accountService);
    }

    @Test
    void creditRejectsReferenceIdThatMatchesRegexButCannotBeParsedAsUuid() throws Exception {
        mockMvc.perform(post("/accounts/ACC-12345678/credit")
                        .header("X-Username", USERNAME)
                        .contentType("application/json")
                        .content("""
                                {"amount":10,"currency":"EUR","referenceId":"11111111-1111-1111-1111-11111111111-"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.referenceId").value("Invalid UUID format"));

        verifyNoInteractions(accountService);
    }

    @Test
    void balanceMapsOwnershipFailureToForbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this account"))
                .when(accountService).validateOwnership(ACCOUNT_NUMBER, "bob");

        mockMvc.perform(get("/accounts/ACC-12345678/balance")
                        .header("X-Username", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You do not own this account"));
    }

    @Test
    void duplicateReferenceMapsToConflict() throws Exception {
        doThrow(new ApplicationException("Duplicate reference ID", HttpStatus.CONFLICT))
                .when(accountService).creditAccount(eq(ACCOUNT_NUMBER), any());

        mockMvc.perform(post("/accounts/ACC-12345678/credit")
                        .header("X-Username", USERNAME)
                        .contentType("application/json")
                        .content("""
                                {"amount":10,"currency":"EUR","referenceId":"11111111-1111-1111-1111-111111111111"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Duplicate reference ID"));
    }

    @Test
    void debitLogsExternallyBeforeAccountServiceDebit() throws Exception {
        mockMvc.perform(post("/accounts/{accountNumber}/debit", ACCOUNT_NUMBER)
                        .header("X-Username", USERNAME)
                        .contentType("application/json")
                        .content("""
                                {"amount":10,"currency":"EUR","referenceId":"11111111-1111-1111-1111-111111111111"}
                                """))
                .andExpect(status().isNoContent());

        InOrder inOrder = inOrder(accountService, externalLoggingService);
        inOrder.verify(accountService).validateOwnership(ACCOUNT_NUMBER, USERNAME);
        inOrder.verify(externalLoggingService).logDebit();
        inOrder.verify(accountService).debitAccount(eq(ACCOUNT_NUMBER), any());
    }

    @Test
    void debitLogsExternallyWhenAccountServiceDebitFails() throws Exception {
        doThrow(new ApplicationException("Insufficient funds", HttpStatus.UNPROCESSABLE_ENTITY))
                .when(accountService).debitAccount(eq(ACCOUNT_NUMBER), any());

        mockMvc.perform(post("/accounts/{accountNumber}/debit", ACCOUNT_NUMBER)
                        .header("X-Username", USERNAME)
                        .contentType("application/json")
                        .content("""
                                {"amount":10,"currency":"EUR","referenceId":"11111111-1111-1111-1111-111111111111"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Insufficient funds"));

        verify(externalLoggingService).logDebit();
    }

    @Test
    void balanceDelegatesOptionalCurrencyFilter() throws Exception {
        when(accountService.getBalances(ACCOUNT_NUMBER, "EUR"))
                .thenReturn(Map.of("EUR", BigDecimal.valueOf(25)));

        mockMvc.perform(get("/accounts/ACC-12345678/balance")
                        .header("X-Username", USERNAME)
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.EUR").value(25));

        verify(accountService).validateOwnership(ACCOUNT_NUMBER, USERNAME);
        verify(accountService).getBalances(ACCOUNT_NUMBER, "EUR");
    }
}
