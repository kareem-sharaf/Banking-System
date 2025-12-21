package com.banking.customer.extra.provider;

import com.banking.core.auth.module.entity.User;
import com.banking.core.auth.repository.UserRepository;
import com.banking.core.exception.CustomerNotFoundException;
import com.banking.core.exception.UserNotFoundException;
import com.banking.customer.module.entity.Customer;
import com.banking.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Customer Query Provider
 *
 * Encapsulates read-only queries for Customer and User domain entities.
 * Provides optimized access to customer-related data without business logic.
 * Returns domain entities for use by facades and other query providers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerQueryProvider {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    /**
     * Find user by username
     *
     * @param username the username to search for
     * @return User entity
     * @throws UserNotFoundException if user is not found
     */
    public User findUserByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }

    /**
     * Find customer by user ID
     *
     * @param userId the user ID to search for
     * @return Customer entity
     * @throws CustomerNotFoundException if customer is not found
     */
    public Customer findCustomerByUserId(Long userId) {
        log.debug("Finding customer by user ID: {}", userId);
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for user ID: " + userId));
    }

    /**
     * Find customer and user data together
     *
     * @param username the username to search for
     * @return Customer entity with associated user data
     * @throws UserNotFoundException if user is not found
     * @throws CustomerNotFoundException if customer is not found
     */
    public Customer findCustomerWithUserByUsername(String username) {
        log.debug("Finding customer with user data by username: {}", username);

        User user = findUserByUsername(username);
        return findCustomerByUserId(user.getId());
    }

    /**
     * Check if customer exists by user ID
     *
     * @param userId the user ID to check
     * @return true if customer exists, false otherwise
     */
    public boolean customerExistsByUserId(Long userId) {
        return customerRepository.existsByUserId(userId);
    }

    /**
     * Find customer by customer number (optional)
     *
     * @param customerNumber the customer number to search for
     * @return Optional containing Customer entity if found
     */
    public Optional<Customer> findCustomerByCustomerNumber(String customerNumber) {
        log.debug("Finding customer by customer number: {}", customerNumber);
        return customerRepository.findByCustomerNumber(customerNumber);
    }
}
