package com.example.bank.service;

import com.example.bank.dto.AccountResponse;
import com.example.bank.dto.CreateAccountRequest;
import com.example.bank.dto.CreditRequest;
import com.example.bank.dto.DebitRequest;
import com.example.bank.dto.ExchangeRequest;
import com.example.bank.entity.Account;
import com.example.bank.exception.ApplicationException;
import com.example.bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final LedgerService ledgerService;
    private final CurrencyExchangeService currencyExchangeService;
    private final ExternalLoggingService externalLoggingService;

    public AccountResponse createAccount(CreateAccountRequest createAccountRequest) {
        if (accountRepository.existsByUsername(createAccountRequest.getUsername())) {
            throw new ApplicationException("Username already exists", HttpStatus.CONFLICT);
        }
        Account account = new Account();
        account.setUsername(createAccountRequest.getUsername());
        account.setAccountNumber(generateAccountNumber());
        Account savedAccount = accountRepository.save(account);

        return new AccountResponse(
                savedAccount.getAccountNumber(),
                savedAccount.getUsername()
        );
    }

    @Transactional
    public void creditAccount(String accountNumber, CreditRequest request) {
        currencyExchangeService.validateCurrency(request.getCurrency());
        Account account = getAccountByAccountNumberForUpdate(accountNumber);
        ledgerService.credit(account.getId(), request);
    }

    @Transactional
    public void debitAccount(String accountNumber, DebitRequest request) {
        currencyExchangeService.validateCurrency(request.getCurrency());
        Account account = getAccountByAccountNumberForUpdate(accountNumber);
        ledgerService.debit(account.getId(), request);
        externalLoggingService.logDebit();

    }

    public Map<String, BigDecimal> getBalances(String accountNumber, String currency) {
        Account account = getAccountByAccountNumber(accountNumber);
        Map<String, BigDecimal> balances;

        if (currency != null) {
            currencyExchangeService.validateCurrency(currency);
            balances = Map.of(
                    currency,
                    ledgerService.getBalance(account.getId(), currency)
            );
        } else {
            balances = ledgerService.getAllBalances(account.getId());
        }
        return balances;

    }

    public void validateOwnership(String accountNumber, String username) {
        Account account = accountRepository
                .findAccountByAccountNumber(accountNumber)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Account not found"
                        )
                );

        if (!account.getUsername().equals(username)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not own this account"
            );
        }
    }

    @Transactional
    public void exchangeCurrency(String accountNumber, ExchangeRequest exchangeRequest) {
        BigDecimal convertedAmount = currencyExchangeService.convert(
                exchangeRequest.getAmount(),
                exchangeRequest.getFromCurrency(),
                exchangeRequest.getToCurrency()
        );
        Account account = getAccountByAccountNumberForUpdate(accountNumber);
        ledgerService.exchangeCurrency(account.getId(), exchangeRequest, convertedAmount);
    }

    private String generateAccountNumber() {
        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        return "ACC-" + randomPart;
    }

    private Account getAccountByAccountNumber(String accountNumber) {
        return accountRepository
                .findAccountByAccountNumber(accountNumber)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Account not found"
                        )
                );
    }

    private Account getAccountByAccountNumberForUpdate(String accountNumber) {
        return accountRepository
                .findAccountByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Account not found"
                        )
                );
    }
}
