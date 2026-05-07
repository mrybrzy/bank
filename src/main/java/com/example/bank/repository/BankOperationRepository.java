package com.example.bank.repository;

import com.example.bank.entity.BankOperation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankOperationRepository extends JpaRepository<BankOperation, Long> {
    boolean existsByReferenceId(String referenceId);
}
