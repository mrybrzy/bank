package com.example.bank.repository;

import com.example.bank.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    boolean existsByReferenceId(String referenceId);
}
