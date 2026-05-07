package com.example.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AccountResponse {

    private String accountNumber;

    private String username;
}