package com.example.bank.controller;

import com.example.bank.dto.CreditRequest;
import com.example.bank.dto.DebitRequest;
import com.example.bank.entity.Account;
import com.example.bank.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController

@RequiredArgsConstructor
public class AccountController {
        private final AccountService accountService;;

        @PostMapping
        public ResponseEntity<Account> createAccount(@Valid @RequestBody Account account) {
                Account createdAccount = accountService.createAccount(account);
                return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
        }

        @PostMapping("/credit")
        public ResponseEntity<Void> creditAccount(@Valid @RequestBody CreditRequest request) {
                accountService.creditAccount(request);
                return ResponseEntity.ok().build();
        }

        @PostMapping("/debit")
        public ResponseEntity<Void> debitAccount(@Valid @RequestBody DebitRequest request) {
                accountService.debitAccount(request);
                return ResponseEntity.ok().build();
        }



}
