package com.example.bank.service;

import com.example.bank.dto.DebitRequest;
import com.example.bank.entity.Account;
import com.example.bank.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountServiceMockTest {
    private static final String ACCOUNT_NUMBER = "ACC-12345678";

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final LedgerService ledgerService = mock(LedgerService.class);
    private final CurrencyExchangeService currencyExchangeService = mock(CurrencyExchangeService.class);
    private final AccountService accountService = new AccountService(
            accountRepository,
            ledgerService,
            currencyExchangeService
    );

    @Test
    void debitValidatesCurrencyLocksAccountAndDelegatesToLedger() {
        Account account = account();
        DebitRequest request = debitRequest("11111111-1111-1111-1111-111111111111");
        when(accountRepository.findAccountByAccountNumberForUpdate(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(account));

        accountService.debitAccount(ACCOUNT_NUMBER, request);

        InOrder inOrder = inOrder(currencyExchangeService, accountRepository, ledgerService);
        inOrder.verify(currencyExchangeService).validateCurrency("EUR");
        inOrder.verify(accountRepository).findAccountByAccountNumberForUpdate(ACCOUNT_NUMBER);
        inOrder.verify(ledgerService).debit(1L, request);
    }

    private static Account account() {
        Account account = new Account();
        account.setId(1L);
        account.setAccountNumber(ACCOUNT_NUMBER);
        account.setUsername("alice");
        return account;
    }

    private static DebitRequest debitRequest(String referenceId) {
        DebitRequest request = new DebitRequest();
        request.setAmount(BigDecimal.TEN);
        request.setCurrency("EUR");
        request.setReferenceId(referenceId);
        return request;
    }
}
