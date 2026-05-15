package com.example.bank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ExchangeRequest {

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal amount;

    @NotBlank
    @Pattern(
            regexp = "^[A-Z]{3}$",
            message = "Currency must be ISO 3-letter code"
    )
    private String fromCurrency;

    @NotBlank
    @Pattern(
            regexp = "^[A-Z]{3}$",
            message = "Currency must be ISO 3-letter code"
    )
    private String toCurrency;

    @NotBlank
    @Pattern(
            regexp = "^[0-9a-fA-F-]{36}$",
            message = "Invalid UUID format"
    )
    private String referenceId;
}
