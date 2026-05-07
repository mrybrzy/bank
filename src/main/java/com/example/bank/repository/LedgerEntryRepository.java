package com.example.bank.repository;

import com.example.bank.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    boolean existsByReferenceId(String referenceId);

    @Query("""
           SELECT COALESCE(SUM(
               CASE
                   WHEN le.type = 'CREDIT' THEN le.amount
                   ELSE -le.amount
               END
           ), 0)
           FROM LedgerEntry le
           WHERE le.accountId = :accountId
           AND le.currency = :currency
           """)
    BigDecimal sumAmountByAccountIdAndCurrency(
            @Param("accountId") Long accountId,
            @Param("currency") LedgerEntry.CurrencyCode currency
    );

    @Query("""
       SELECT le.currency,
              COALESCE(SUM(
                  CASE
                      WHEN le.type = 'CREDIT' THEN le.amount
                      ELSE -le.amount
                  END
              ), 0)
       FROM LedgerEntry le
       WHERE le.accountId = :accountId
       GROUP BY le.currency
       """)
    List<Object[]> sumAmountByAccountId(@Param("accountId") Long accountId);}
