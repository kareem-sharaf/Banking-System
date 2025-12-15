package com.banking.config;

import com.banking.dto.RegisterRequest;
import com.banking.entity.*;
import com.banking.repository.*;
import com.banking.service.KeycloakAdminService;
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
                createAccountType("Deposit Account", "DEPOSIT", "Time deposit account"));

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
}
