package com.banking.repository;

import com.banking.entity.Customer;
import com.banking.enums.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerNumber(String customerNumber);
    Optional<Customer> findByUserId(Long userId);
    List<Customer> findByStatus(CustomerStatus status);
    boolean existsByCustomerNumber(String customerNumber);
    boolean existsByUserId(Long userId);
}

