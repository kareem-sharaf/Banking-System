package com.banking.repository;

import com.banking.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountTypeRepository extends JpaRepository<AccountType, Long> {
    Optional<AccountType> findByCode(String code);
    Optional<AccountType> findByName(String name);
    boolean existsByCode(String code);
    boolean existsByName(String name);
}

