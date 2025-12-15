package com.banking.repository;

import com.banking.entity.Account;
import com.banking.enums.AccountState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

