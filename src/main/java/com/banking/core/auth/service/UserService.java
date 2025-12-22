package com.banking.core.auth.service;

import com.banking.core.auth.dto.RegisterRequest;
import com.banking.core.auth.dto.CreateUserRequest;
import com.banking.core.auth.dto.UpdateUserRequest;
import com.banking.core.auth.dto.UserResponse;
import com.banking.core.auth.module.entity.LoginHistory;
import com.banking.core.auth.module.entity.User;
import com.banking.core.auth.module.entity.Role;
import com.banking.core.auth.repository.LoginHistoryRepository;
import com.banking.core.auth.repository.RoleRepository;
import com.banking.core.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing users in local database
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private RoleRepository roleRepository;

    /**
     * Create a local user after registration in Keycloak
     */
    @Transactional
    public User createLocalUser(RegisterRequest registerRequest, String keycloakId) {
        try {
            // Check if user already exists
            if (userRepository.existsByUsername(registerRequest.getUsername()) ||
                    userRepository.existsByEmail(registerRequest.getEmail()) ||
                    userRepository.existsByKeycloakId(keycloakId)) {
                logger.warn("User already exists: username={}, email={}, keycloakId={}",
                        registerRequest.getUsername(), registerRequest.getEmail(), keycloakId);
                return null;
            }

            User user = new User();
            user.setKeycloakId(keycloakId);
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setFirstName(registerRequest.getFirstName());
            user.setLastName(registerRequest.getLastName());
            user.setPhoneNumber(registerRequest.getPhoneNumber());
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());

            // Set default role (CUSTOMER) if role repository is available
            roleRepository.findByName(registerRequest.getRole() != null ? registerRequest.getRole() : "CUSTOMER")
                    .ifPresentOrElse(
                            user::setRole,
                            () -> {
                                // If role not found, try to get CUSTOMER role
                                roleRepository.findByName("CUSTOMER")
                                        .ifPresent(user::setRole);
                            });

            User savedUser = userRepository.save(user);
            logger.info("Local user created successfully: id={}, username={}, keycloakId={}",
                    savedUser.getId(), savedUser.getUsername(), savedUser.getKeycloakId());

            return savedUser;
        } catch (Exception e) {
            logger.error("Error creating local user", e);
            throw e;
        }
    }

    /**
     * Update login history for a user
     */
    @Transactional
    public void updateLoginHistory(User user, String ipAddress, String userAgent, boolean success,
            String failureReason) {
        try {
            LoginHistory loginHistory = new LoginHistory();
            loginHistory.setUser(user);
            loginHistory.setLoginTime(LocalDateTime.now());
            loginHistory.setIpAddress(ipAddress);
            loginHistory.setUserAgent(userAgent);
            loginHistory.setSuccess(success);
            loginHistory.setFailureReason(failureReason);

            loginHistoryRepository.save(loginHistory);
            logger.info("Login history recorded: user={}, success={}", user.getUsername(), success);
        } catch (Exception e) {
            logger.error("Error recording login history", e);
        }
    }

    /**
     * Update logout time in login history
     */
    @Transactional
    public void updateLogoutHistory(User user) {
        try {
            // Find the most recent successful login for this user
            Optional<LoginHistory> latestLogin = loginHistoryRepository
                    .findByUserAndSuccessOrderByLoginTimeDesc(user, true)
                    .stream()
                    .findFirst();

            if (latestLogin.isPresent()) {
                LoginHistory loginHistory = latestLogin.get();
                if (loginHistory.getLogoutTime() == null) {
                    loginHistory.setLogoutTime(LocalDateTime.now());
                    loginHistoryRepository.save(loginHistory);
                    logger.info("Logout time updated for user: {}", user.getUsername());
                }
            }
        } catch (Exception e) {
            logger.error("Error updating logout history", e);
        }
    }

    /**
     * Update last login time for user
     */
    @Transactional
    public void updateLastLoginAt(User user) {
        try {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        } catch (Exception e) {
            logger.error("Error updating last login time", e);
        }
    }

    /**
     * Find user by Keycloak ID
     */
    public Optional<User> findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId);
    }

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Extract IP address from request
     */
    public String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Extract user agent from request
     */
    public String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 500) {
            return userAgent.substring(0, 500);
        }
        return userAgent;
    }

    /**
     * Get all users
     */
    public List<UserResponse> getAllUsers() {
        logger.info("Retrieving all users");
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get user by ID
     */
    public UserResponse getUserById(Long id) {
        logger.info("Retrieving user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
        return mapToUserResponse(user);
    }

    /**
     * Get user by username
     */
    public UserResponse getUserByUsername(String username) {
        logger.info("Retrieving user with username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        return mapToUserResponse(user);
    }

    /**
     * Get user by email
     */
    public UserResponse getUserByEmail(String email) {
        logger.info("Retrieving user with email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return mapToUserResponse(user);
    }

    /**
     * Create a new user (admin operation)
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        logger.info("Creating new user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("User with username '" + request.getUsername() + "' already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with email '" + request.getEmail() + "' already exists");
        }

        // Get role
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + request.getRoleId()));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        user.setActive(request.getIsActive() != null ? request.getIsActive() : true);
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        logger.info("Created user with ID: {}", savedUser.getId());

        return mapToUserResponse(savedUser);
    }

    /**
     * Update user
     */
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        logger.info("Updating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        // Check for conflicts if username is being updated
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new IllegalArgumentException("User with username '" + request.getUsername() + "' already exists");
            }
            user.setUsername(request.getUsername());
        }

        // Check for conflicts if email is being updated
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("User with email '" + request.getEmail() + "' already exists");
            }
            user.setEmail(request.getEmail());
        }

        // Update other fields if provided
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + request.getRoleId()));
            user.setRole(role);
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
            user.setActive(request.getIsActive());
        }

        User updatedUser = userRepository.save(user);
        logger.info("Updated user with ID: {}", updatedUser.getId());

        return mapToUserResponse(updatedUser);
    }

    /**
     * Delete user
     */
    @Transactional
    public void deleteUser(Long id) {
        logger.info("Deleting user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        // Check if user has associated customer
        if (user.getCustomer() != null) {
            throw new IllegalArgumentException("Cannot delete user with associated customer account");
        }

        userRepository.delete(user);
        logger.info("Deleted user with ID: {}", id);
    }

    /**
     * Check if user exists by username
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if user exists by email
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Get users by role
     */
    public List<UserResponse> getUsersByRoleId(Long roleId) {
        logger.info("Retrieving users with role ID: {}", roleId);
        return userRepository.findByRoleId(roleId).stream()
                .map(this::mapToUserResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .isActive(user.getIsActive())
                .roleId(user.getRole() != null ? user.getRole().getId() : null)
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .active(user.isActive())
                .build();
    }
}
