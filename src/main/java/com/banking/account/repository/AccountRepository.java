package com.banking.account.repository;

import com.banking.account.module.entity.Account;
import com.banking.core.enums.AccountState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(Long customerId);

    List<Account> findByState(AccountState state);

    List<Account> findByParentAccountId(Long parentAccountId);

    List<Account> findByAccountTypeId(Long accountTypeId);

    boolean existsByAccountNumber(String accountNumber);

    /**
     * Find accounts by account type and state
     */
    List<Account> findByAccountTypeIdAndState(Long accountTypeId, AccountState state);

    /**
     * Find accounts that haven't had interest calculated since a specific date
     */
    @Query("SELECT a FROM Account a WHERE a.state = :state AND (a.lastInterestCalculation IS NULL OR a.lastInterestCalculation < :date)")
    List<Account> findAccountsNeedingInterestCalculation(
            @Param("state") AccountState state,
            @Param("date") LocalDate date);
}
