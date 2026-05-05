package com.example.bank.service;

import com.example.bank.dto.CreditRequest;
import com.example.bank.dto.DebitRequest;
import com.example.bank.entity.Account;
import com.example.bank.exception.ApplicationException;
import com.example.bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public void creditAccount(CreditRequest request) {
        if (!accountRepository.existsById(request.getAccountId())) {
            throw new ApplicationException("Account not found");
        }
        ledgerService.credit(request);

    }

    public void debitAccount(DebitRequest request) {
        if (!accountRepository.existsById(request.getAccountId())) {
            throw new ApplicationException("Account not found");
        }
        ledgerService.debit(request);

    }
}
