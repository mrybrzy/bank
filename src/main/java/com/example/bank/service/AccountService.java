package com.example.bank.service;

import com.example.bank.dto.AccountResponse;
import com.example.bank.dto.CreateAccountRequest;
import com.example.bank.dto.CreditRequest;
import com.example.bank.dto.DebitRequest;
import com.example.bank.entity.Account;
import com.example.bank.entity.LedgerEntry;
import com.example.bank.exception.ApplicationException;
import com.example.bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final LedgerService ledgerService;
    private final ExternalLoggingService externalLoggingService;

    public AccountResponse createAccount(CreateAccountRequest createAccountRequest) {
        if (accountRepository.existsByUsername(createAccountRequest.getUsername())) {
            throw new ApplicationException("Username already exists");
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

    private String generateAccountNumber() {
        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        return "ACC-" + randomPart;
    }

    public void creditAccount(String accountNumber, CreditRequest request) {
        Account account = getAccountByAccountNumber(accountNumber);
        ledgerService.credit(account.getId(), request);
    }

    public void debitAccount(String accountNumber, DebitRequest request) {
        Account account = getAccountByAccountNumber(accountNumber);
        externalLoggingService.logDebit();
        ledgerService.debit(account.getId(), request);

    }

    public Map<LedgerEntry.CurrencyCode, BigDecimal> getBalances(String accountNumber, LedgerEntry.CurrencyCode currency) {
        Account account = getAccountByAccountNumber(accountNumber);
        Map<LedgerEntry.CurrencyCode, BigDecimal> balances;

        if (currency != null) {
            balances = Map.of(
                    currency,
                    ledgerService.getBalance(account.getId(), currency)
            );
        } else {
            balances = ledgerService.getAllBalances(account.getId());
        }
        return balances;

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


    private void currency() {
        //todo
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
}
