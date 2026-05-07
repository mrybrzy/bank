package com.example.bank.service;

import com.example.bank.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurrencyExchangeService {
    private static final Map<String, BigDecimal>
            EXCHANGE_RATES = Map.of(
            "EUR", BigDecimal.ONE,
            "USD", BigDecimal.valueOf(1.08),
            "SEK", BigDecimal.valueOf(11.5),
            "GBP", BigDecimal.valueOf(0.86)
    );

    public void validateCurrency(String currency) {
        if (!EXCHANGE_RATES.containsKey(currency)) {
            throw new ApplicationException("Unsupported currency: " + currency);
        }
    }

    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        validateCurrency(fromCurrency);
        validateCurrency(toCurrency);

        BigDecimal fromRate = EXCHANGE_RATES.get(fromCurrency);
        BigDecimal toRate = EXCHANGE_RATES.get(toCurrency);

        BigDecimal eurAmount = amount.divide(fromRate, 2, RoundingMode.HALF_UP);
        return eurAmount.multiply(toRate).setScale(2, RoundingMode.HALF_UP);

    }
}
