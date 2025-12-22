package com.banking.core.config;

import com.banking.core.auth.dto.RegisterRequest;
import com.banking.core.auth.module.entity.Role;
import com.banking.core.auth.module.entity.Permission;
import com.banking.core.auth.module.entity.RolePermission;
import com.banking.core.auth.module.entity.User;
import com.banking.account.module.entity.Account;
import com.banking.account.module.entity.AccountType;
import com.banking.customer.module.entity.Customer;
import com.banking.transaction.module.entity.ScheduledTransaction;
import com.banking.transaction.module.entity.Transaction;
import com.banking.support.module.entity.SupportTicket;
import com.banking.core.enums.*;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.AccountTypeRepository;
import com.banking.customer.repository.CustomerRepository;
import com.banking.transaction.repository.ScheduledTransactionRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.support.repository.SupportTicketRepository;
import com.banking.core.auth.repository.RoleRepository;
import com.banking.core.auth.repository.PermissionRepository;
import com.banking.core.auth.repository.RolePermissionRepository;
import com.banking.core.auth.repository.UserRepository;
import com.banking.core.auth.service.KeycloakAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Data Seeder for routine tables
 * Runs automatically when the application starts
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ScheduledTransactionRepository scheduledTransactionRepository;

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private KeycloakAdminService keycloakAdminService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting routine tables seeding...");

        seedRoles();
        seedPermissions();
        seedAccountTypes();
        seedRolePermissions();
        seedAdminUser();

        // Seed sample data for testing (only in development)
        if (isDevelopmentEnvironment()) {
            seedSampleData();
        }

        logger.info("Routine tables seeding completed successfully!");
    }

    /**
     * Seed Roles table
     */
    private void seedRoles() {
        logger.info("Seeding Roles table...");

        Role adminRole = new Role();
        adminRole.setName("System Administrator");
        adminRole.setCode("ADMIN");
        adminRole.setDescription("Full system access and permissions");

        Role managerRole = new Role();
        managerRole.setName("Manager");
        managerRole.setCode("MANAGER");
        managerRole.setDescription("Operations and staff management");

        Role tellerRole = new Role();
        tellerRole.setName("Teller");
        tellerRole.setCode("TELLER");
        tellerRole.setDescription("Daily transaction processing");

        Role customerRole = new Role();
        customerRole.setName("Customer");
        customerRole.setCode("CUSTOMER");
        customerRole.setDescription("Standard customer permissions");

        List<Role> roles = Arrays.asList(adminRole, managerRole, tellerRole, customerRole);

        for (Role role : roles) {
            if (!roleRepository.existsByCode(role.getCode())) {
                roleRepository.save(role);
                logger.info("Added role: {}", role.getName());
            } else {
                logger.debug("Role already exists: {}", role.getCode());
            }
        }
    }

    /**
     * Seed Permissions table
     */
    private void seedPermissions() {
        logger.info("Seeding Permissions table...");

        List<Permission> permissions = Arrays.asList(
                createPermission("View Users", "USER", "VIEW", "View list of users"),
                createPermission("Create User", "USER", "CREATE", "Add new user"),
                createPermission("Update User", "USER", "UPDATE", "Update user information"),
                createPermission("Delete User", "USER", "DELETE", "Delete user"),

                createPermission("View Customers", "CUSTOMER", "VIEW", "View list of customers"),
                createPermission("Create Customer", "CUSTOMER", "CREATE", "Add new customer"),
                createPermission("Update Customer", "CUSTOMER", "UPDATE", "Update customer information"),
                createPermission("Delete Customer", "CUSTOMER", "DELETE", "Delete customer"),

                createPermission("View Accounts", "ACCOUNT", "VIEW", "View list of accounts"),
                createPermission("Create Account", "ACCOUNT", "CREATE", "Open new account"),
                createPermission("Update Account", "ACCOUNT", "UPDATE", "Update account information"),
                createPermission("Close Account", "ACCOUNT", "CLOSE", "Close account"),

                createPermission("View Transactions", "TRANSACTION", "VIEW", "View list of transactions"),
                createPermission("Create Transaction", "TRANSACTION", "CREATE", "Execute financial transaction"),
                createPermission("Cancel Transaction", "TRANSACTION", "CANCEL", "Cancel transaction"),
                createPermission("Approve Transaction", "TRANSACTION", "APPROVE", "Approve transaction"),

                createPermission("View Roles", "ROLE", "VIEW", "View list of roles"),
                createPermission("Create Role", "ROLE", "CREATE", "Add new role"),
                createPermission("Update Role", "ROLE", "UPDATE", "Update role information"),
                createPermission("Delete Role", "ROLE", "DELETE", "Delete role"),

                createPermission("View Permissions", "PERMISSION", "VIEW", "View list of permissions"),
                createPermission("Manage Permissions", "PERMISSION", "MANAGE", "Manage permissions"),

                createPermission("View Account Types", "ACCOUNT_TYPE", "VIEW", "View list of account types"),
                createPermission("Create Account Type", "ACCOUNT_TYPE", "CREATE", "Add new account type"),
                createPermission("Update Account Type", "ACCOUNT_TYPE", "UPDATE", "Update account type"),
                createPermission("Delete Account Type", "ACCOUNT_TYPE", "DELETE", "Delete account type"),

                createPermission("View Tickets", "TICKET", "VIEW", "View list of support tickets"),
                createPermission("Create Ticket", "TICKET", "CREATE", "Create new support ticket"),
                createPermission("Update Ticket", "TICKET", "UPDATE", "Update support ticket"),
                createPermission("Close Ticket", "TICKET", "CLOSE", "Close support ticket"),

                createPermission("View Reports", "REPORT", "VIEW", "View list of reports"),
                createPermission("Create Report", "REPORT", "CREATE", "Create new report"),
                createPermission("Export Report", "REPORT", "EXPORT", "Export report"),

                createPermission("View Audit Logs", "AUDIT_LOG", "VIEW", "View audit logs"),

                createPermission("View Notifications", "NOTIFICATION", "VIEW", "View list of notifications"),
                createPermission("Send Notification", "NOTIFICATION", "SEND", "Send notification"),

                createPermission("View Scheduled Transactions", "SCHEDULED_TRANSACTION", "VIEW",
                        "View list of scheduled transactions"),
                createPermission("Create Scheduled Transaction", "SCHEDULED_TRANSACTION", "CREATE",
                        "Create scheduled transaction"),
                createPermission("Update Scheduled Transaction", "SCHEDULED_TRANSACTION", "UPDATE",
                        "Update scheduled transaction"),
                createPermission("Delete Scheduled Transaction", "SCHEDULED_TRANSACTION", "DELETE",
                        "Delete scheduled transaction"));

        for (Permission permission : permissions) {
            if (!permissionRepository.existsByName(permission.getName())) {
                permissionRepository.save(permission);
                logger.info("Added permission: {}", permission.getName());
            } else {
                logger.debug("Permission already exists: {}", permission.getName());
            }
        }
    }

    /**
     * Create a new permission
     */
    private Permission createPermission(String name, String resource, String action, String description) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setResource(resource);
        permission.setAction(action);
        permission.setDescription(description);
        return permission;
    }

    /**
     * Seed Account Types table
     */
    private void seedAccountTypes() {
        logger.info("Seeding Account Types table...");

        List<AccountType> accountTypes = Arrays.asList(
                createAccountType("Savings Account", "SAVINGS", "Regular savings account with monthly interest"),
                createAccountType("Checking Account", "CHECKING", "Checking account for daily operations"),
                createAccountType("Investment Account", "INVESTMENT", "Investment account with higher returns"),
                createAccountType("Child Savings Account", "CHILD_SAVINGS", "Savings account designed for children"),
                createAccountType("Business Account", "BUSINESS", "Business account for companies"),
                createAccountType("Deposit Account", "DEPOSIT", "Time deposit account"),
                createAccountType("Loan Account", "LOAN", "Personal loan account with variable interest"),
                createAccountType("Mortgage Account", "MORTGAGE", "Mortgage loan account"),
                createAccountType("Credit Card", "CREDIT_CARD", "Credit card account"),
                createAccountType("Certificate of Deposit", "CD", "High-yield certificate of deposit"));

        for (AccountType accountType : accountTypes) {
            if (!accountTypeRepository.existsByCode(accountType.getCode())) {
                accountTypeRepository.save(accountType);
                logger.info("Added account type: {}", accountType.getName());
            } else {
                logger.debug("Account type already exists: {}", accountType.getCode());
            }
        }
    }

    /**
     * Create a new account type
     */
    private AccountType createAccountType(String name, String code, String description) {
        AccountType accountType = new AccountType();
        accountType.setName(name);
        accountType.setCode(code);
        accountType.setDescription(description);
        return accountType;
    }

    /**
     * Seed Role Permissions table
     */
    private void seedRolePermissions() {
        logger.info("Seeding Role Permissions table...");

        Role adminRole = roleRepository.findByCode("ADMIN").orElse(null);
        Role managerRole = roleRepository.findByCode("MANAGER").orElse(null);
        Role tellerRole = roleRepository.findByCode("TELLER").orElse(null);
        Role customerRole = roleRepository.findByCode("CUSTOMER").orElse(null);

        if (adminRole == null || managerRole == null || tellerRole == null || customerRole == null) {
            logger.error("Required roles not found!");
            return;
        }

        // Admin permissions (all permissions)
        List<Permission> allPermissions = permissionRepository.findAll();
        for (Permission permission : allPermissions) {
            if (!rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getId(), permission.getId())) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRole(adminRole);
                rolePermission.setPermission(permission);
                rolePermissionRepository.save(rolePermission);
            }
        }
        logger.info("Assigned all permissions to Admin");

        // Manager permissions
        List<String> managerPermissions = Arrays.asList(
                "View Users", "View Customers", "Create Customer", "Update Customer",
                "View Accounts", "Create Account", "Update Account", "Close Account",
                "View Transactions", "Create Transaction", "Approve Transaction",
                "View Roles", "View Permissions",
                "View Account Types", "Create Account Type", "Update Account Type",
                "View Tickets", "Update Ticket", "Close Ticket",
                "View Reports", "Create Report", "Export Report",
                "View Audit Logs", "View Notifications", "Send Notification",
                "View Scheduled Transactions", "Create Scheduled Transaction", "Update Scheduled Transaction");
        assignPermissionsToRole(managerRole, managerPermissions);
        logger.info("Assigned permissions to Manager");

        // Teller permissions
        List<String> tellerPermissions = Arrays.asList(
                "View Customers", "View Accounts", "View Transactions", "Create Transaction",
                "View Tickets", "Create Ticket", "Update Ticket",
                "View Notifications", "View Scheduled Transactions");
        assignPermissionsToRole(tellerRole, tellerPermissions);
        logger.info("Assigned permissions to Teller");

        // Customer permissions
        List<String> customerPermissions = Arrays.asList(
                "View Accounts", "View Transactions", "Create Transaction",
                "View Tickets", "Create Ticket",
                "View Notifications", "View Scheduled Transactions", "Create Scheduled Transaction",
                "Update Scheduled Transaction", "Delete Scheduled Transaction");
        assignPermissionsToRole(customerRole, customerPermissions);
        logger.info("Assigned permissions to Customer");
    }

    /**
     * Assign permissions to a role
     */
    private void assignPermissionsToRole(Role role, List<String> permissionNames) {
        for (String permissionName : permissionNames) {
            Permission permission = permissionRepository.findByName(permissionName).orElse(null);
            if (permission != null) {
                if (!rolePermissionRepository.existsByRoleIdAndPermissionId(role.getId(), permission.getId())) {
                    RolePermission rolePermission = new RolePermission();
                    rolePermission.setRole(role);
                    rolePermission.setPermission(permission);
                    rolePermissionRepository.save(rolePermission);
                }
            }
        }
    }

    /**
     * Create default admin user
     */
    private void seedAdminUser() {
        logger.info("Checking for default admin user...");

        String adminUsername = "admin";
        String adminPassword = "admin123";

        // Check if user exists in local database
        if (!userRepository.existsByUsername(adminUsername)) {
            Role adminRole = roleRepository.findByCode("ADMIN").orElse(null);
            if (adminRole != null) {
                // Create user in local database
                User adminUser = new User();
                adminUser.setUsername(adminUsername);
                adminUser.setEmail("admin@banking.com");
                // Note: Password is managed by Keycloak, not stored locally
                adminUser.setFirstName("System");
                adminUser.setLastName("Administrator");
                adminUser.setPhoneNumber("+966500000000");
                adminUser.setDateOfBirth(LocalDate.of(1980, 1, 1));
                adminUser.setActive(true);
                adminUser.setRole(adminRole);
                adminUser.setCreatedAt(LocalDateTime.now());

                userRepository.save(adminUser);
                logger.info("Created default admin user in local database (admin/admin123)");
            } else {
                logger.warn("Admin role not found, skipping default user creation");
            }
        } else {
            logger.debug("Admin user already exists in local database");
        }

        // Create user in Keycloak if it doesn't exist
        try {
            if (!keycloakAdminService.usernameExists(adminUsername)) {
                RegisterRequest adminRegisterRequest = new RegisterRequest();
                adminRegisterRequest.setUsername(adminUsername);
                adminRegisterRequest.setEmail("admin@banking.com");
                adminRegisterRequest.setPassword(adminPassword);
                adminRegisterRequest.setFirstName("System");
                adminRegisterRequest.setLastName("Administrator");
                adminRegisterRequest.setRole("ADMIN");

                Map<String, Object> createResult = keycloakAdminService.createUser(adminRegisterRequest);
                if ((Boolean) createResult.getOrDefault("success", false)) {
                    logger.info("Created default admin user in Keycloak (admin/admin123)");
                } else {
                    logger.warn("Failed to create admin user in Keycloak: {}", createResult.get("message"));
                }
            } else {
                logger.debug("Admin user already exists in Keycloak");
            }
        } catch (Exception e) {
            logger.warn("Could not create admin user in Keycloak (Keycloak may not be running): {}", e.getMessage());
        }
    }

    /**
     * Check if we're running in development environment
     */
    private boolean isDevelopmentEnvironment() {
        String[] activeProfiles = {"dev", "development", "local", "test"};
        // In a real application, you'd check Spring profiles here
        // For now, we'll assume development if no production profile is set
        return true; // Always seed sample data for development
    }

    /**
     * Seed sample data for development/testing
     */
    private void seedSampleData() {
        logger.info("Seeding sample data for development...");

        try {
            seedSampleUsers();
            seedSampleCustomers();
            seedSampleAccounts();
            seedSampleTransactions();
            seedSampleScheduledTransactions();
            seedSampleSupportTickets();

            logger.info("Sample data seeding completed successfully!");
        } catch (Exception e) {
            logger.warn("Failed to seed sample data: {}", e.getMessage());
        }
    }

    /**
     * Seed sample users
     */
    private void seedSampleUsers() {
        logger.info("Seeding sample users...");

        Role customerRole = roleRepository.findByCode("CUSTOMER").orElse(null);
        Role tellerRole = roleRepository.findByCode("TELLER").orElse(null);
        Role managerRole = roleRepository.findByCode("MANAGER").orElse(null);

        if (customerRole == null || tellerRole == null || managerRole == null) {
            logger.warn("Required roles not found for sample users");
            return;
        }

        // Sample customers
        List<User> sampleUsers = Arrays.asList(
            createSampleUser("john.doe", "john.doe@email.com", "John", "Doe", customerRole, "johndoe"),
            createSampleUser("jane.smith", "jane.smith@email.com", "Jane", "Smith", customerRole, "janesmith"),
            createSampleUser("bob.johnson", "bob.johnson@email.com", "Bob", "Johnson", customerRole, "bobjohnson"),
            createSampleUser("alice.brown", "alice.brown@email.com", "Alice", "Brown", tellerRole, "alicebrown"),
            createSampleUser("charlie.wilson", "charlie.wilson@email.com", "Charlie", "Wilson", managerRole, "charliewilson")
        );

        for (User user : sampleUsers) {
            if (!userRepository.existsByUsername(user.getUsername())) {
                try {
                    // Create in Keycloak first
                    RegisterRequest registerRequest = new RegisterRequest();
                    registerRequest.setUsername(user.getUsername());
                    registerRequest.setEmail(user.getEmail());
                    registerRequest.setPassword(user.getUsername() + "123"); // Simple password for testing
                    registerRequest.setFirstName(user.getFirstName());
                    registerRequest.setLastName(user.getLastName());
                    registerRequest.setRole(user.getRole().getCode());

                    Map<String, Object> result = keycloakAdminService.createUser(registerRequest);
                    if ((Boolean) result.getOrDefault("success", false)) {
                        // Now create in local database
                        userRepository.save(user);
                        logger.info("Created sample user: {}", user.getUsername());
                    }
                } catch (Exception e) {
                    logger.debug("Could not create user {} in Keycloak: {}", user.getUsername(), e.getMessage());
                }
            }
        }
    }

    /**
     * Seed sample customers linked to users
     */
    private void seedSampleCustomers() {
        logger.info("Seeding sample customers...");

        List<String[]> customerData = Arrays.asList(
            new String[]{"john.doe", "CUST-001"},
            new String[]{"jane.smith", "CUST-002"},
            new String[]{"bob.johnson", "CUST-003"}
        );

        for (String[] data : customerData) {
            String username = data[0];
            String customerNumber = data[1];

            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null && !customerRepository.existsByUserId(user.getId())) {
                Customer customer = new Customer();
                customer.setUser(user);
                customer.setCustomerNumber(customerNumber);
                customer.setStatus(CustomerStatus.ACTIVE);

                customerRepository.save(customer);
                logger.info("Created sample customer: {} for user {}", customerNumber, username);
            }
        }
    }

    /**
     * Seed sample accounts for customers
     */
    private void seedSampleAccounts() {
        logger.info("Seeding sample accounts...");

        List<String[]> accountData = Arrays.asList(
            new String[]{"john.doe", "SAVINGS", "10000.00"},
            new String[]{"john.doe", "CHECKING", "2500.00"},
            new String[]{"jane.smith", "SAVINGS", "15000.00"},
            new String[]{"jane.smith", "INVESTMENT", "50000.00"},
            new String[]{"bob.johnson", "CHECKING", "7500.00"},
            new String[]{"bob.johnson", "BUSINESS", "25000.00"}
        );

        for (String[] data : accountData) {
            String username = data[0];
            String accountTypeCode = data[1];
            String balanceStr = data[2];

            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                Customer customer = customerRepository.findByUserId(user.getId()).orElse(null);
                AccountType accountType = accountTypeRepository.findByCode(accountTypeCode).orElse(null);

                if (customer != null && accountType != null) {
                    Account account = new Account();
                    account.setAccountNumber(generateAccountNumber());
                    account.setCustomer(customer);
                    account.setAccountType(accountType);
                    account.setBalance(new java.math.BigDecimal(balanceStr));
                    account.setCurrency("SAR");
                    account.setState(AccountState.ACTIVE);

                    accountRepository.save(account);
                    logger.info("Created sample account: {} ({}) for customer {}",
                        account.getAccountNumber(), accountTypeCode, username);
                }
            }
        }
    }

    /**
     * Seed sample transactions
     */
    private void seedSampleTransactions() {
        logger.info("Seeding sample transactions...");

        // Get some accounts for transactions
        List<Account> accounts = accountRepository.findAll();
        if (accounts.size() < 2) {
            logger.debug("Not enough accounts for sample transactions");
            return;
        }

        // Sample deposits
        for (int i = 0; i < Math.min(3, accounts.size()); i++) {
            Account account = accounts.get(i);
            createSampleTransaction(account, null, TransactionType.DEPOSIT,
                new java.math.BigDecimal("1000.00"), "Initial deposit");
        }

        // Sample transfers between accounts
        if (accounts.size() >= 2) {
            Account fromAccount = accounts.get(0);
            Account toAccount = accounts.get(1);

            if (fromAccount.getBalance().compareTo(new java.math.BigDecimal("500.00")) >= 0) {
                createSampleTransaction(fromAccount, toAccount, TransactionType.TRANSFER,
                    new java.math.BigDecimal("500.00"), "Sample transfer");
            }
        }

        logger.info("Sample transactions created");
    }

    /**
     * Seed sample scheduled transactions
     */
    private void seedSampleScheduledTransactions() {
        logger.info("Seeding sample scheduled transactions...");

        // Get sample accounts and transactions
        List<Account> accounts = accountRepository.findAll();
        List<Transaction> transactions = transactionRepository.findAll();

        if (accounts.isEmpty() || transactions.isEmpty()) {
            logger.debug("Not enough data for sample scheduled transactions");
            return;
        }

        // Create a sample scheduled transaction
        Transaction templateTransaction = transactions.get(0);
        Account account = accounts.get(0);

        ScheduledTransaction scheduledTx = new ScheduledTransaction();
        scheduledTx.setTransactionTemplate(templateTransaction);
        scheduledTx.setScheduleType(ScheduleType.MONTHLY);
        scheduledTx.setNextExecutionDate(LocalDateTime.now().plusDays(30));
        scheduledTx.setIsActive(true);

        scheduledTransactionRepository.save(scheduledTx);
        logger.info("Created sample scheduled transaction for monthly execution");
    }

    /**
     * Seed sample support tickets
     */
    private void seedSampleSupportTickets() {
        logger.info("Seeding sample support tickets...");

        List<Customer> customers = customerRepository.findAll();
        if (customers.isEmpty()) {
            logger.debug("No customers found for sample tickets");
            return;
        }

        List<String[]> ticketData = Arrays.asList(
            new String[]{"Account balance not updating", "My account balance shows incorrect amount after recent deposit",
                "ACCOUNT_ISSUE", "MEDIUM"},
            new String[]{"Transaction not appearing", "I made a transfer yesterday but it doesn't show in my history",
                "TRANSACTION_ISSUE", "HIGH"},
            new String[]{"Login issues", "Having trouble logging into my account from mobile app",
                "TECHNICAL_SUPPORT", "LOW"}
        );

        for (int i = 0; i < Math.min(ticketData.size(), customers.size()); i++) {
            String[] data = ticketData.get(i);
            Customer customer = customers.get(i);

            SupportTicket ticket = new SupportTicket();
            ticket.setTicketNumber("TICKET-" + String.format("%03d", i + 1));
            ticket.setCustomer(customer);
            ticket.setSubject(data[0]);
            ticket.setDescription(data[1]);
            ticket.setCategory(TicketCategory.valueOf(data[2]));
            ticket.setPriority(TicketPriority.valueOf(data[3]));
            ticket.setStatus(TicketStatus.OPEN);

            supportTicketRepository.save(ticket);
            logger.info("Created sample support ticket: {}", ticket.getSubject());
        }
    }

    /**
     * Helper method to create sample user
     */
    private User createSampleUser(String username, String email, String firstName,
                                String lastName, Role role, String keycloakId) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber("+9665" + String.format("%08d", username.hashCode() % 100000000));
        user.setDateOfBirth(LocalDate.of(1980 + (username.hashCode() % 20), 1 + (username.hashCode() % 12), 1 + (username.hashCode() % 28)));
        user.setActive(true);
        user.setRole(role);
        return user;
    }

    /**
     * Helper method to create sample transaction
     */
    private void createSampleTransaction(Account fromAccount, Account toAccount,
                                       TransactionType type, java.math.BigDecimal amount, String description) {
        Transaction transaction = new Transaction();
        transaction.setTransactionNumber("TXN-" + System.currentTimeMillis() + "-" + fromAccount.getId());
        transaction.setAmount(amount);
        transaction.setTransactionType(type);
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setDescription(description);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setTransactionDate(LocalDateTime.now().minusDays((long) (Math.random() * 30)));

        // Update account balances
        if (type == TransactionType.DEPOSIT || type == TransactionType.TRANSFER) {
            fromAccount.setBalance(fromAccount.getBalance().add(amount));
        } else if (type == TransactionType.WITHDRAWAL) {
            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        }

        transactionRepository.save(transaction);
        accountRepository.save(fromAccount);
        if (toAccount != null) {
            if (type == TransactionType.TRANSFER) {
                toAccount.setBalance(toAccount.getBalance().add(amount));
            }
            accountRepository.save(toAccount);
        }
    }

    /**
     * Generate account number
     */
    private String generateAccountNumber() {
        return "ACC-" + String.format("%010d", (long) (Math.random() * 10000000000L));
    }
}
