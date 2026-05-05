package com.example.bank.dto;

import com.example.bank.entity.LedgerEntry;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreditRequest {
    @NotNull
    private Long accountId;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal amount;

    @NotNull
    private LedgerEntry.CurrencyCode currency;

    @NotBlank
    @Pattern(
            regexp = "^[0-9a-fA-F-]{36}$",
            message = "Invalid UUID format"
    )
    private String referenceId; // pattern and String, so in case not valid UUID it will be said in response

}