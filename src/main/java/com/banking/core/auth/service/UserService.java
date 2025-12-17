package com.banking.core.auth.service;

import com.banking.core.auth.dto.RegisterRequest;
import com.banking.core.auth.module.entity.LoginHistory;
import com.banking.core.auth.module.entity.User;
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
import java.util.Optional;

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
}
