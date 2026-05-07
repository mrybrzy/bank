package com.example.bank.service;

import com.example.bank.dto.CreditRequest;
import com.example.bank.dto.DebitRequest;
import com.example.bank.entity.Account;
import com.example.bank.entity.LedgerEntry;
import com.example.bank.exception.ApplicationException;
import com.example.bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final LedgerService ledgerService;

    public Account createAccount(Account account) {
        if (accountRepository.existsByUsername(account.getUsername())) {
            throw new ApplicationException("Username already exists");
        }
        return accountRepository.save(account);
    }

    public void creditAccount(Long accountId, CreditRequest request) {
        if (!accountRepository.existsById(accountId)) {
            throw new ApplicationException("Account not found");
        }
        ledgerService.credit(accountId, request);

    }

    public void debitAccount(Long accountId, DebitRequest request) {
        if (!accountRepository.existsById(accountId)) {
            throw new ApplicationException("Account not found");
        }
        //todo make api request
        ledgerService.debit(accountId, request);

    }

    public Map<LedgerEntry.CurrencyCode, BigDecimal> getBalances(Long accountId, LedgerEntry.CurrencyCode currency) {
        if (!accountRepository.existsById(accountId)) {
            throw new ApplicationException("Account not found");
        }
        Map<LedgerEntry.CurrencyCode, BigDecimal> balances;

        if (currency != null) {
            balances = Map.of(
                    currency,
                    ledgerService.getBalance(accountId, currency)
            );
        } else {
            balances = ledgerService.getAllBalances(accountId);
        }
        return balances;

    }

    private void currency() {
    //todo
    }

    public void validateOwnership(Long accountId, String username) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApplicationException("Account not found"));
        if (!account.getUsername().equals(username)) {
            throw new ApplicationException("Unauthorized");
        }
    }
}
