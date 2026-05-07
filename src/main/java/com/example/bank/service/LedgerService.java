package com.example.bank.service;

import com.example.bank.dto.CreditRequest;
import com.example.bank.dto.DebitRequest;
import com.example.bank.dto.ExchangeRequest;
import com.example.bank.entity.BankOperation;
import com.example.bank.entity.LedgerEntry;
import com.example.bank.exception.ApplicationException;
import com.example.bank.repository.BankOperationRepository;
import com.example.bank.repository.LedgerEntryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final BankOperationRepository bankOperationRepository;


    public void credit(Long accountId, CreditRequest request) {
        validatePositiveAmount(request.getAmount());
        registerOperation(request.getReferenceId(), BankOperation.OperationType.CREDIT);
        createCreditEntry(accountId, request.getCurrency(), request.getAmount(), request.getReferenceId());
    }

    public void debit(Long accountId, DebitRequest request) {
        validatePositiveAmount(request.getAmount());
        boolean hasSufficientFunds = getBalance(accountId, request.getCurrency()).compareTo(request.getAmount()) >= 0;
        if (!hasSufficientFunds) {
            throw new ApplicationException("Insufficient funds");
        }
        registerOperation(request.getReferenceId(), BankOperation.OperationType.DEBIT);
        createDebitEntry(accountId, request.getCurrency(), request.getAmount(), request.getReferenceId());
    }

    public BigDecimal getBalance(Long accountId, String currency) {
        return ledgerEntryRepository.sumAmountByAccountIdAndCurrency(accountId, currency);
    }

    public Map<String, BigDecimal> getAllBalances(Long accountId) {
        List<Object[]> rows =
                ledgerEntryRepository.sumAmountByAccountId(accountId);

        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (BigDecimal) row[1]
                ));
    }

    public void exchangeCurrency(Long accountId, ExchangeRequest request, BigDecimal convertedAmount) {
        validatePositiveAmount(request.getAmount());
        boolean hasSufficientFunds = getBalance(accountId, request.getFromCurrency()).compareTo(request.getAmount()) >= 0;

        if (!hasSufficientFunds) {
            throw new ApplicationException("Insufficient funds");
        }
        createDebitEntry(accountId, request.getFromCurrency(), request.getAmount(), request.getReferenceId());
        registerOperation(request.getReferenceId(), BankOperation.OperationType.EXCHANGE);
        createCreditEntry(accountId, request.getToCurrency(), convertedAmount, request.getReferenceId());
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApplicationException("Amount must be positive");
        }
    }

    private void registerOperation(String referenceId, BankOperation.OperationType type) {
        if (bankOperationRepository.existsByReferenceId(referenceId)) {
            throw new ApplicationException("Duplicate reference ID");
        }

        BankOperation operation = new BankOperation();
        operation.setReferenceId(referenceId);
        operation.setType(type);

        try {
            bankOperationRepository.saveAndFlush(operation);
        } catch (DataIntegrityViolationException ex) {
            throw new ApplicationException("Duplicate reference ID");
        }
    }

    private void createCreditEntry(Long accountId, String currency, BigDecimal amount, String referenceId) {
        LedgerEntry entry = new LedgerEntry();
        entry.setAccountId(accountId);
        entry.setAmount(amount);
        entry.setCurrency(currency);
        entry.setReferenceId(referenceId);
        entry.setType(LedgerEntry.LedgerEntryType.CREDIT);
        ledgerEntryRepository.save(entry);
    }

    private void createDebitEntry(Long accountId, String currency, BigDecimal amount, String referenceId) {
        LedgerEntry entry = new LedgerEntry();
        entry.setAccountId(accountId);
        entry.setAmount(amount);
        entry.setCurrency(currency);
        entry.setReferenceId(referenceId);
        entry.setType(LedgerEntry.LedgerEntryType.DEBIT);
        ledgerEntryRepository.save(entry);
    }
}
