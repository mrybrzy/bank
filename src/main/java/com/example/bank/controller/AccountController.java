package com.example.bank.controller;

import com.example.bank.dto.CreditRequest;
import com.example.bank.dto.DebitRequest;
import com.example.bank.entity.Account;
import com.example.bank.entity.LedgerEntry;
import com.example.bank.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    ;

    @PostMapping
    public ResponseEntity<Account> createAccount(@Valid @RequestBody Account account) {
        Account createdAccount = accountService.createAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    @PostMapping("/{id}/credit")
    public ResponseEntity<Void> creditAccount(@PathVariable("id") Long accountId,
                                              @RequestHeader("X-Username") String username,
                                              @Valid @RequestBody CreditRequest request) {
        accountService.validateOwnership(accountId, username);
        accountService.creditAccount(accountId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/debit")
    public ResponseEntity<Void> debitAccount(@PathVariable("id") Long accountId,
                                             @RequestHeader("X-Username") String username,
                                             @Valid @RequestBody DebitRequest request) {
        accountService.validateOwnership(accountId, username);
        accountService.debitAccount(accountId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<Map<LedgerEntry.CurrencyCode, BigDecimal>> getAccountBalance(
            @PathVariable("id") Long accountId,
            @RequestHeader("X-Username") String username,
            @RequestParam(required = false) LedgerEntry.CurrencyCode currency
    ) {
        accountService.validateOwnership(accountId, username);
        Map<LedgerEntry.CurrencyCode, BigDecimal> balances = accountService.getBalances(accountId, currency);
        return ResponseEntity.ok(balances);
    }
}
