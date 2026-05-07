package com.example.bank;

import com.example.bank.dto.CreateAccountRequest;
import com.example.bank.dto.CreditRequest;
import com.example.bank.dto.DebitRequest;
import com.example.bank.exception.ApplicationException;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.BankOperationRepository;
import com.example.bank.repository.LedgerEntryRepository;
import com.example.bank.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:transaction-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TransactionConsistencyTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BankOperationRepository bankOperationRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @BeforeEach
    void cleanDatabase() {
        ledgerEntryRepository.deleteAll();
        bankOperationRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void successfulDebitChangesLedgerAndRegistersOperation() {
        String accountNumber = createAccount();
        accountService.creditAccount(accountNumber, credit("11111111-1111-1111-1111-111111111111"));
        accountService.debitAccount(
                accountNumber,
                debit("22222222-2222-2222-2222-222222222222")
        );

        assertThat(accountService.getBalances(accountNumber, null).get("EUR")).isEqualByComparingTo("90.00");
        assertThat(ledgerEntryRepository.count()).isEqualTo(2);
        assertThat(bankOperationRepository.count()).isEqualTo(2);
    }

    @Test
    void insufficientFundsRollsBackRegisteredDebitOperation() {
        String accountNumber = createAccount();

        assertThatThrownBy(() -> accountService.debitAccount(
                accountNumber,
                debit("33333333-3333-3333-3333-333333333333")
        ))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Insufficient funds");

        assertThat(ledgerEntryRepository.count()).isZero();
        assertThat(bankOperationRepository.count()).isZero();
    }

    private String createAccount() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setUsername("alice");
        return accountService.createAccount(request).getAccountNumber();
    }

    private static CreditRequest credit(String referenceId) {
        CreditRequest request = new CreditRequest();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("EUR");
        request.setReferenceId(referenceId);
        return request;
    }

    private static DebitRequest debit(String referenceId) {
        DebitRequest request = new DebitRequest();
        request.setAmount(BigDecimal.TEN);
        request.setCurrency("EUR");
        request.setReferenceId(referenceId);
        return request;
    }
}
