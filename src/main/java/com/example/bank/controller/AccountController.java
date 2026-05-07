package com.example.bank.controller;

import com.example.bank.dto.AccountResponse;
import com.example.bank.dto.CreateAccountRequest;
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
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest createAccountRequest) {
        AccountResponse createdAccount = accountService.createAccount(createAccountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    @PostMapping("/{accountNumber}/credit")
    public ResponseEntity<Void> creditAccount(@PathVariable("accountNumber") String accountNumber,
                                              @RequestHeader("X-Username") String username,
                                              @Valid @RequestBody CreditRequest request) {
        accountService.validateOwnership(accountNumber, username);
        accountService.creditAccount(accountNumber, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{accountNumber}/debit")
    public ResponseEntity<Void> debitAccount(@PathVariable("accountNumber") String accountNumber,
                                             @RequestHeader("X-Username") String username,
                                             @Valid @RequestBody DebitRequest request) {
        accountService.validateOwnership(accountNumber, username);
        accountService.debitAccount(accountNumber, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<Map<LedgerEntry.CurrencyCode, BigDecimal>> getAccountBalance(
            @PathVariable("accountNumber") String accountNumber,
            @RequestHeader("X-Username") String username,
            @RequestParam(required = false) LedgerEntry.CurrencyCode currency
    ) {
        accountService.validateOwnership(accountNumber, username);
        Map<LedgerEntry.CurrencyCode, BigDecimal> balances = accountService.getBalances(accountNumber, currency);
        return ResponseEntity.ok(balances);
    }
}
