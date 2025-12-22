package com.banking.customer.service;

import com.banking.core.enums.CustomerStatus;
import com.banking.customer.dto.CreateCustomerRequest;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.dto.UpdateCustomerRequest;
import com.banking.customer.module.entity.Customer;
import com.banking.customer.repository.CustomerRepository;
import com.banking.core.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Customer Service
 *
 * Service layer for Customer Management (CRUD).
 *
 * @author Banking System
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    /**
     * Create a new customer
     */
    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        logger.info("Creating new customer for user ID: {}", request.getUserId());

        // Check if user exists
        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + request.getUserId()));

        // Check if customer already exists for this user
        if (customerRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("Customer already exists for user ID: " + request.getUserId());
        }

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setCustomerNumber(generateCustomerNumber());
        customer.setJoinDate(LocalDateTime.now());
        customer.setStatus(request.getStatus() != null ? request.getStatus() : CustomerStatus.ACTIVE);

        Customer savedCustomer = customerRepository.save(customer);
        logger.info("Created customer with ID: {}", savedCustomer.getId());

        return mapToCustomerResponse(savedCustomer);
    }

    /**
     * Get all customers
     */
    public List<CustomerResponse> getAllCustomers() {
        logger.info("Retrieving all customers");
        return customerRepository.findAll().stream()
                .map(this::mapToCustomerResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get customer by ID
     */
    public CustomerResponse getCustomerById(Long id) {
        logger.info("Retrieving customer with ID: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));
        return mapToCustomerResponse(customer);
    }

    /**
     * Get customer by user ID
     */
    public CustomerResponse getCustomerByUserId(Long userId) {
        logger.info("Retrieving customer for user ID: {}", userId);
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found for user ID: " + userId));
        return mapToCustomerResponse(customer);
    }

    /**
     * Get customer by customer number
     */
    public CustomerResponse getCustomerByCustomerNumber(String customerNumber) {
        logger.info("Retrieving customer with customer number: {}", customerNumber);
        Customer customer = customerRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with customer number: " + customerNumber));
        return mapToCustomerResponse(customer);
    }

    /**
     * Get customers by status
     */
    public List<CustomerResponse> getCustomersByStatus(CustomerStatus status) {
        logger.info("Retrieving customers with status: {}", status);
        return customerRepository.findByStatus(status).stream()
                .map(this::mapToCustomerResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update customer
     */
    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        logger.info("Updating customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));

        // Update status if provided
        if (request.getStatus() != null) {
            customer.setStatus(request.getStatus());
        }

        Customer updatedCustomer = customerRepository.save(customer);
        logger.info("Updated customer with ID: {}", updatedCustomer.getId());

        return mapToCustomerResponse(updatedCustomer);
    }

    /**
     * Delete customer
     */
    @Transactional
    public void deleteCustomer(Long id) {
        logger.info("Deleting customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));

        // Check if customer has associated accounts
        if (!customer.getAccounts().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete customer with associated accounts. Account count: " + customer.getAccounts().size());
        }

        // Check if customer has associated support tickets
        if (!customer.getSupportTickets().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete customer with associated support tickets. Ticket count: " + customer.getSupportTickets().size());
        }

        customerRepository.delete(customer);
        logger.info("Deleted customer with ID: {}", id);
    }

    /**
     * Check if customer exists by user ID
     */
    public boolean existsByUserId(Long userId) {
        return customerRepository.existsByUserId(userId);
    }

    /**
     * Check if customer exists by customer number
     */
    public boolean existsByCustomerNumber(String customerNumber) {
        return customerRepository.existsByCustomerNumber(customerNumber);
    }

    private CustomerResponse mapToCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .userId(customer.getUser() != null ? customer.getUser().getId() : null)
                .customerNumber(customer.getCustomerNumber())
                .joinDate(customer.getJoinDate())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .accountCount(customer.getAccounts() != null ? customer.getAccounts().size() : 0)
                .username(customer.getUser() != null ? customer.getUser().getUsername() : null)
                .email(customer.getUser() != null ? customer.getUser().getEmail() : null)
                .firstName(customer.getUser() != null ? customer.getUser().getFirstName() : null)
                .lastName(customer.getUser() != null ? customer.getUser().getLastName() : null)
                .build();
    }

    private String generateCustomerNumber() {
        return "CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
