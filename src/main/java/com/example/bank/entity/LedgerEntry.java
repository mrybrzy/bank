package com.example.bank.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long accountId;

    @Enumerated(EnumType.STRING)
    private CurrencyCode currency;

    private BigDecimal amount; // positive

    @Enumerated(EnumType.STRING)
    private LedgerEntryType type;

    private String referenceId; // for idempotency

    private Instant createdAt = Instant.now();

    public enum CurrencyCode {
        EUR, USD, GBP //todo make better
    }

    public enum LedgerEntryType {
        CREDIT,
        DEBIT
    }
}
