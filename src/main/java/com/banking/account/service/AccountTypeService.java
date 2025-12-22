package com.banking.account.service;

import com.banking.account.dto.AccountTypeResponse;
import com.banking.account.dto.CreateAccountTypeRequest;
import com.banking.account.dto.UpdateAccountTypeRequest;
import com.banking.account.module.entity.AccountType;
import com.banking.account.repository.AccountTypeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Account Type Service
 *
 * Service layer for Account Type Management (CRUD).
 *
 * @author Banking System
 */
@Service
@RequiredArgsConstructor
public class AccountTypeService {

    private static final Logger logger = LoggerFactory.getLogger(AccountTypeService.class);

    private final AccountTypeRepository accountTypeRepository;

    /**
     * Create a new account type
     */
    @Transactional
    public AccountTypeResponse createAccountType(CreateAccountTypeRequest request) {
        logger.info("Creating new account type: {}", request.getName());

        // Check if code already exists
        if (accountTypeRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Account type with code '" + request.getCode() + "' already exists");
        }

        // Check if name already exists
        if (accountTypeRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Account type with name '" + request.getName() + "' already exists");
        }

        AccountType accountType = new AccountType();
        accountType.setName(request.getName());
        accountType.setCode(request.getCode());
        accountType.setDescription(request.getDescription());

        AccountType savedAccountType = accountTypeRepository.save(accountType);
        logger.info("Created account type with ID: {}", savedAccountType.getId());

        return mapToAccountTypeResponse(savedAccountType);
    }

    /**
     * Get all account types
     */
    public List<AccountTypeResponse> getAllAccountTypes() {
        logger.info("Retrieving all account types");
        return accountTypeRepository.findAll().stream()
                .map(this::mapToAccountTypeResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get account type by ID
     */
    public AccountTypeResponse getAccountTypeById(Long id) {
        logger.info("Retrieving account type with ID: {}", id);
        AccountType accountType = accountTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account type not found with ID: " + id));
        return mapToAccountTypeResponse(accountType);
    }

    /**
     * Get account type by code
     */
    public AccountTypeResponse getAccountTypeByCode(String code) {
        logger.info("Retrieving account type with code: {}", code);
        AccountType accountType = accountTypeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Account type not found with code: " + code));
        return mapToAccountTypeResponse(accountType);
    }

    /**
     * Update account type
     */
    @Transactional
    public AccountTypeResponse updateAccountType(Long id, UpdateAccountTypeRequest request) {
        logger.info("Updating account type with ID: {}", id);

        AccountType accountType = accountTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account type not found with ID: " + id));

        // Check for conflicts if name is being updated
        if (request.getName() != null && !request.getName().equals(accountType.getName())) {
            if (accountTypeRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("Account type with name '" + request.getName() + "' already exists");
            }
            accountType.setName(request.getName());
        }

        // Check for conflicts if code is being updated
        if (request.getCode() != null && !request.getCode().equals(accountType.getCode())) {
            if (accountTypeRepository.existsByCode(request.getCode())) {
                throw new IllegalArgumentException("Account type with code '" + request.getCode() + "' already exists");
            }
            accountType.setCode(request.getCode());
        }

        // Update description if provided
        if (request.getDescription() != null) {
            accountType.setDescription(request.getDescription());
        }

        AccountType updatedAccountType = accountTypeRepository.save(accountType);
        logger.info("Updated account type with ID: {}", updatedAccountType.getId());

        return mapToAccountTypeResponse(updatedAccountType);
    }

    /**
     * Delete account type
     */
    @Transactional
    public void deleteAccountType(Long id) {
        logger.info("Deleting account type with ID: {}", id);

        AccountType accountType = accountTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account type not found with ID: " + id));

        // Check if account type has associated accounts
        if (!accountType.getAccounts().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete account type with associated accounts. Account count: " + accountType.getAccounts().size());
        }

        accountTypeRepository.delete(accountType);
        logger.info("Deleted account type with ID: {}", id);
    }

    /**
     * Check if account type exists by code
     */
    public boolean existsByCode(String code) {
        return accountTypeRepository.existsByCode(code);
    }

    /**
     * Check if account type exists by name
     */
    public boolean existsByName(String name) {
        return accountTypeRepository.existsByName(name);
    }

    private AccountTypeResponse mapToAccountTypeResponse(AccountType accountType) {
        return AccountTypeResponse.builder()
                .id(accountType.getId())
                .name(accountType.getName())
                .code(accountType.getCode())
                .description(accountType.getDescription())
                .accountCount(accountType.getAccounts() != null ? accountType.getAccounts().size() : 0)
                .build();
    }
}
