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
            EXCHANGE_RATES = Map.ofEntries(
            Map.entry("EUR", BigDecimal.valueOf(1.0)),
            Map.entry("USD", BigDecimal.valueOf(1.08)),
            Map.entry("SEK", BigDecimal.valueOf(11.5)),
            Map.entry("GBP", BigDecimal.valueOf(0.86)),
            Map.entry("JPY", BigDecimal.valueOf(169.2)),
            Map.entry("CHF", BigDecimal.valueOf(0.94)),
            Map.entry("NOK", BigDecimal.valueOf(11.8)),
            Map.entry("DKK", BigDecimal.valueOf(7.46)),
            Map.entry("PLN", BigDecimal.valueOf(4.29)),
            Map.entry("CAD", BigDecimal.valueOf(1.47)),
            Map.entry("AUD", BigDecimal.valueOf(1.64)),
            Map.entry("CNY", BigDecimal.valueOf(7.82))
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

        BigDecimal eurAmount = amount.divide(fromRate, 2, BigDecimal.ROUND_HALF_UP);
        return eurAmount.multiply(toRate).setScale(2, RoundingMode.HALF_UP);

    }
}
