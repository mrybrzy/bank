package com.example.bank.service;

import com.example.bank.dto.CreditRequest;
import com.example.bank.dto.DebitRequest;
import com.example.bank.entity.LedgerEntry;
import com.example.bank.exception.ApplicationException;
import com.example.bank.repository.LedgerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Transactional
public class LedgerService {
    private final LedgerRepository ledgerRepository;

    public LedgerService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    public void credit(CreditRequest request) { // panen raha peale
        if (ledgerRepository.existsByReferenceId(request.getReferenceId())) {
            throw new ApplicationException("Duplicate reference ID");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApplicationException("Amount must be positive");
        }
        LedgerEntry entry = new LedgerEntry();
        entry.setAccountId(request.getAccountId());
        entry.setAmount(request.getAmount());
        entry.setCurrency(request.getCurrency());
        entry.setReferenceId(request.getReferenceId());
        entry.setType(LedgerEntry.LedgerEntryType.CREDIT);
        ledgerRepository.save(entry);

    }

    public void debit(DebitRequest request) { // votan raha valja

    }
}
