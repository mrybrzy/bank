package com.example.bank.service;

import com.example.bank.dto.CreditRequest;
import com.example.bank.dto.DebitRequest;
import com.example.bank.entity.LedgerEntry;
import com.example.bank.exception.ApplicationException;
import com.example.bank.repository.LedgerEntryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class LedgerService {
    private final LedgerEntryRepository ledgerEntryRepository;


    public void credit(Long accountId, CreditRequest request) { // panen raha peale
        if (ledgerEntryRepository.existsByReferenceId(request.getReferenceId())) {
            throw new ApplicationException("Duplicate reference ID");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApplicationException("Amount must be positive");
        }
        LedgerEntry entry = new LedgerEntry();
        entry.setAccountId(accountId);
        entry.setAmount(request.getAmount());
        entry.setCurrency(request.getCurrency());
        entry.setReferenceId(request.getReferenceId());
        entry.setType(LedgerEntry.LedgerEntryType.CREDIT);
        ledgerEntryRepository.save(entry);

    }

    public void debit(Long accountId, DebitRequest request) { // votan raha valja
        if (ledgerEntryRepository.existsByReferenceId(request.getReferenceId())) {
            throw new ApplicationException("Duplicate reference ID");
        }

        boolean hasSufficientFunds = getBalance(accountId, request.getCurrency()).compareTo(request.getAmount()) >= 0;
        if (!hasSufficientFunds) {
            throw new ApplicationException("Insufficient funds");
        }
        LedgerEntry entry = new LedgerEntry();
        entry.setAccountId(accountId);
        entry.setAmount(request.getAmount());
        entry.setCurrency(request.getCurrency());
        entry.setReferenceId(request.getReferenceId());
        entry.setType(LedgerEntry.LedgerEntryType.DEBIT);
        ledgerEntryRepository.save(entry);
    }

    public BigDecimal getBalance(Long accountId, LedgerEntry.CurrencyCode currency) {
        return ledgerEntryRepository.sumAmountByAccountIdAndCurrency(accountId, currency);
    }

     public Map<LedgerEntry.CurrencyCode, BigDecimal> getAllBalances(Long accountId) {
         List<Object[]> rows =
                 ledgerEntryRepository.sumAmountByAccountId(accountId);

         return rows.stream()
                 .collect(Collectors.toMap(
                         row -> (LedgerEntry.CurrencyCode) row[0],
                         row -> (BigDecimal) row[1]
                 ));
     }
}
