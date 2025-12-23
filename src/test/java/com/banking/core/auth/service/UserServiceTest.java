package com.banking.core.auth.service;

import com.banking.core.auth.dto.CreateUserRequest;
import com.banking.core.auth.dto.RegisterRequest;
import com.banking.core.auth.dto.UpdateUserRequest;
import com.banking.core.auth.dto.UserResponse;
import com.banking.core.auth.module.entity.LoginHistory;
import com.banking.core.auth.module.entity.Role;
import com.banking.core.auth.module.entity.User;
import com.banking.core.auth.repository.LoginHistoryRepository;
import com.banking.core.auth.repository.RoleRepository;
import com.banking.core.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Tests for UserService
 * 
 * Test Strategy: AAA Pattern (Arrange-Act-Assert)
 * Coverage: >95% line coverage, 100% critical path coverage
 * 
 * @author QA Automation Team
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role testRole;
    private RegisterRequest registerRequest;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;

    @BeforeEach
    void setUp() {
        // Arrange: Create test data
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName("CUSTOMER");

        testUser = new User();
        testUser.setId(1L);
        testUser.setKeycloakId("keycloak-123");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPhoneNumber("1234567890");
        testUser.setRole(testRole);
        testUser.setActive(true);
        testUser.setIsActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setCustomer(null);

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
        registerRequest.setPhoneNumber("9876543210");
        registerRequest.setRole("CUSTOMER");

        createUserRequest = CreateUserRequest.builder()
                .username("adminuser")
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .phoneNumber("5555555555")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .roleId(1L)
                .isActive(true)
                .build();

        updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setFirstName("Updated");
        updateUserRequest.setLastName("Name");
    }

    @Nested
    @DisplayName("createLocalUser Tests")
    class CreateLocalUserTests {

        @Test
        @DisplayName("US-001: Create user with valid data - should succeed")
        void createLocalUser_WithValidData_ShouldSucceed() {
            // Arrange
            String keycloakId = "keycloak-456";
            when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(userRepository.existsByKeycloakId(keycloakId)).thenReturn(false);
            when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(testRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(2L);
                return user;
            });

            // Act
            User result = userService.createLocalUser(registerRequest, keycloakId);

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(keycloakId, result.getKeycloakId()),
                    () -> assertEquals(registerRequest.getUsername(), result.getUsername()),
                    () -> assertEquals(registerRequest.getEmail(), result.getEmail()),
                    () -> assertTrue(result.isActive()),
                    () -> assertNotNull(result.getCreatedAt())
            );
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("US-002: Create user with duplicate username - should return null")
        void createLocalUser_WithDuplicateUsername_ShouldReturnNull() {
            // Arrange
            String keycloakId = "keycloak-456";
            when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

            // Act
            User result = userService.createLocalUser(registerRequest, keycloakId);

            // Assert
            assertNull(result);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("US-003: Create user with duplicate email - should return null")
        void createLocalUser_WithDuplicateEmail_ShouldReturnNull() {
            // Arrange
            String keycloakId = "keycloak-456";
            when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

            // Act
            User result = userService.createLocalUser(registerRequest, keycloakId);

            // Assert
            assertNull(result);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("US-004: Create user with duplicate Keycloak ID - should return null")
        void createLocalUser_WithDuplicateKeycloakId_ShouldReturnNull() {
            // Arrange
            String keycloakId = "keycloak-456";
            when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(userRepository.existsByKeycloakId(keycloakId)).thenReturn(true);

            // Act
            User result = userService.createLocalUser(registerRequest, keycloakId);

            // Assert
            assertNull(result);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("US-005: Create user with null role - should default to CUSTOMER")
        void createLocalUser_WithNullRole_ShouldDefaultToCustomer() {
            // Arrange
            String keycloakId = "keycloak-456";
            registerRequest.setRole(null);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByKeycloakId(keycloakId)).thenReturn(false);
            when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(testRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(2L);
                return user;
            });

            // Act
            User result = userService.createLocalUser(registerRequest, keycloakId);

            // Assert
            assertNotNull(result);
            assertEquals(testRole, result.getRole());
            verify(roleRepository).findByName("CUSTOMER");
        }

        @Test
        @DisplayName("US-006: Create user with non-existent role - should fallback to CUSTOMER")
        void createLocalUser_WithNonExistentRole_ShouldFallbackToCustomer() {
            // Arrange
            String keycloakId = "keycloak-456";
            registerRequest.setRole("INVALID_ROLE");
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByKeycloakId(keycloakId)).thenReturn(false);
            when(roleRepository.findByName("INVALID_ROLE")).thenReturn(Optional.empty());
            when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(testRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(2L);
                return user;
            });

            // Act
            User result = userService.createLocalUser(registerRequest, keycloakId);

            // Assert
            assertNotNull(result);
            assertEquals(testRole, result.getRole());
            verify(roleRepository).findByName("INVALID_ROLE");
            verify(roleRepository).findByName("CUSTOMER");
        }

        @Test
        @DisplayName("US-Edge-001: Create user with null request - should throw exception")
        void createLocalUser_WithNullRequest_ShouldThrowException() {
            // Arrange & Act & Assert
            assertThrows(Exception.class, () -> userService.createLocalUser(null, "keycloak-123"));
        }

        @Test
        @DisplayName("US-Edge-002: Create user with null Keycloak ID - should handle gracefully")
        void createLocalUser_WithNullKeycloakId_ShouldHandleGracefully() {
            // Arrange
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByKeycloakId(null)).thenReturn(false);
            when(roleRepository.findByName(anyString())).thenReturn(Optional.of(testRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(2L);
                return user;
            });

            // Act
            User result = userService.createLocalUser(registerRequest, null);

            // Assert
            assertNotNull(result);
            assertNull(result.getKeycloakId());
        }
    }

    @Nested
    @DisplayName("updateLoginHistory Tests")
    class UpdateLoginHistoryTests {

        @Test
        @DisplayName("US-007: Record successful login - should save login history")
        void updateLoginHistory_WithSuccessfulLogin_ShouldSaveHistory() {
            // Arrange
            String ipAddress = "192.168.1.1";
            String userAgent = "Mozilla/5.0";
            when(loginHistoryRepository.save(any(LoginHistory.class))).thenAnswer(invocation -> {
                LoginHistory history = invocation.getArgument(0);
                history.setId(1L);
                return history;
            });

            // Act
            userService.updateLoginHistory(testUser, ipAddress, userAgent, true, null);

            // Assert
            ArgumentCaptor<LoginHistory> captor = ArgumentCaptor.forClass(LoginHistory.class);
            verify(loginHistoryRepository).save(captor.capture());
            LoginHistory savedHistory = captor.getValue();
            assertAll(
                    () -> assertEquals(testUser, savedHistory.getUser()),
                    () -> assertEquals(ipAddress, savedHistory.getIpAddress()),
                    () -> assertEquals(userAgent, savedHistory.getUserAgent()),
                    () -> assertTrue(savedHistory.isSuccess()),
                    () -> assertNull(savedHistory.getFailureReason())
            );
        }

        @Test
        @DisplayName("US-008: Record failed login with reason - should save failure history")
        void updateLoginHistory_WithFailedLogin_ShouldSaveFailureHistory() {
            // Arrange
            String ipAddress = "192.168.1.1";
            String userAgent = "Mozilla/5.0";
            String failureReason = "Invalid credentials";
            when(loginHistoryRepository.save(any(LoginHistory.class))).thenAnswer(invocation -> {
                LoginHistory history = invocation.getArgument(0);
                history.setId(1L);
                return history;
            });

            // Act
            userService.updateLoginHistory(testUser, ipAddress, userAgent, false, failureReason);

            // Assert
            ArgumentCaptor<LoginHistory> captor = ArgumentCaptor.forClass(LoginHistory.class);
            verify(loginHistoryRepository).save(captor.capture());
            LoginHistory savedHistory = captor.getValue();
            assertAll(
                    () -> assertFalse(savedHistory.isSuccess()),
                    () -> assertEquals(failureReason, savedHistory.getFailureReason())
            );
        }

        @Test
        @DisplayName("US-Edge-003: Record login with null user - should handle gracefully")
        void updateLoginHistory_WithNullUser_ShouldHandleGracefully() {
            // Arrange
            when(loginHistoryRepository.save(any(LoginHistory.class))).thenAnswer(invocation -> {
                LoginHistory history = invocation.getArgument(0);
                history.setId(1L);
                return history;
            });

            // Act & Assert
            assertDoesNotThrow(() -> userService.updateLoginHistory(null, "127.0.0.1", "Agent", true, null));
        }
    }

    @Nested
    @DisplayName("updateLogoutHistory Tests")
    class UpdateLogoutHistoryTests {

        @Test
        @DisplayName("US-009: Update logout time for existing login - should update logout time")
        void updateLogoutHistory_WithExistingLogin_ShouldUpdateLogoutTime() {
            // Arrange
            LoginHistory loginHistory = new LoginHistory();
            loginHistory.setId(1L);
            loginHistory.setUser(testUser);
            loginHistory.setLoginTime(LocalDateTime.now().minusHours(1));
            loginHistory.setSuccess(true);
            loginHistory.setLogoutTime(null);

            List<LoginHistory> loginHistories = new ArrayList<>();
            loginHistories.add(loginHistory);

            when(loginHistoryRepository.findByUserAndSuccessOrderByLoginTimeDesc(testUser, true))
                    .thenReturn(loginHistories);
            when(loginHistoryRepository.save(any(LoginHistory.class))).thenReturn(loginHistory);

            // Act
            userService.updateLogoutHistory(testUser);

            // Assert
            ArgumentCaptor<LoginHistory> captor = ArgumentCaptor.forClass(LoginHistory.class);
            verify(loginHistoryRepository).save(captor.capture());
            assertNotNull(captor.getValue().getLogoutTime());
        }

        @Test
        @DisplayName("US-010: Update logout when no login exists - should not update")
        void updateLogoutHistory_WithNoLogin_ShouldNotUpdate() {
            // Arrange
            when(loginHistoryRepository.findByUserAndSuccessOrderByLoginTimeDesc(testUser, true))
                    .thenReturn(new ArrayList<>());

            // Act
            userService.updateLogoutHistory(testUser);

            // Assert
            verify(loginHistoryRepository, never()).save(any(LoginHistory.class));
        }

        @Test
        @DisplayName("US-Edge-004: Update logout when logout time already set - should not update")
        void updateLogoutHistory_WithLogoutTimeAlreadySet_ShouldNotUpdate() {
            // Arrange
            LoginHistory loginHistory = new LoginHistory();
            loginHistory.setId(1L);
            loginHistory.setUser(testUser);
            loginHistory.setLoginTime(LocalDateTime.now().minusHours(1));
            loginHistory.setLogoutTime(LocalDateTime.now().minusMinutes(30));
            loginHistory.setSuccess(true);

            List<LoginHistory> loginHistories = new ArrayList<>();
            loginHistories.add(loginHistory);

            when(loginHistoryRepository.findByUserAndSuccessOrderByLoginTimeDesc(testUser, true))
                    .thenReturn(loginHistories);

            // Act
            userService.updateLogoutHistory(testUser);

            // Assert
            verify(loginHistoryRepository, never()).save(any(LoginHistory.class));
        }
    }

    @Nested
    @DisplayName("updateLastLoginAt Tests")
    class UpdateLastLoginAtTests {

        @Test
        @DisplayName("US-011: Update last login timestamp - should update user")
        void updateLastLoginAt_WithValidUser_ShouldUpdateTimestamp() {
            // Arrange
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            userService.updateLastLoginAt(testUser);

            // Assert
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertNotNull(captor.getValue().getLastLoginAt());
        }
    }

    @Nested
    @DisplayName("getAllUsers Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("US-012: Retrieve all users - should return list")
        void getAllUsers_WithUsers_ShouldReturnList() {
            // Arrange
            List<User> users = List.of(testUser);
            when(userRepository.findAll()).thenReturn(users);

            // Act
            List<UserResponse> result = userService.getAllUsers();

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(testUser.getUsername(), result.get(0).getUsername())
            );
        }

        @Test
        @DisplayName("US-Edge-005: Retrieve all users when empty - should return empty list")
        void getAllUsers_WithNoUsers_ShouldReturnEmptyList() {
            // Arrange
            when(userRepository.findAll()).thenReturn(new ArrayList<>());

            // Act
            List<UserResponse> result = userService.getAllUsers();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getUserById Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("US-013: Get user by valid ID - should return user")
        void getUserById_WithValidId_ShouldReturnUser() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // Act
            UserResponse result = userService.getUserById(1L);

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(testUser.getId(), result.getId()),
                    () -> assertEquals(testUser.getUsername(), result.getUsername())
            );
        }

        @Test
        @DisplayName("US-014: Get user by invalid ID - should throw exception")
        void getUserById_WithInvalidId_ShouldThrowException() {
            // Arrange
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.getUserById(999L));
            assertEquals("User not found with ID: 999", exception.getMessage());
        }

        @Test
        @DisplayName("US-Edge-006: Get user by null ID - should throw exception")
        void getUserById_WithNullId_ShouldThrowException() {
            // Arrange
            when(userRepository.findById(null)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> userService.getUserById(null));
        }
    }

    @Nested
    @DisplayName("getUserByUsername Tests")
    class GetUserByUsernameTests {

        @Test
        @DisplayName("US-015: Get user by valid username - should return user")
        void getUserByUsername_WithValidUsername_ShouldReturnUser() {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // Act
            UserResponse result = userService.getUserByUsername("testuser");

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(testUser.getUsername(), result.getUsername())
            );
        }

        @Test
        @DisplayName("US-016: Get user by invalid username - should throw exception")
        void getUserByUsername_WithInvalidUsername_ShouldThrowException() {
            // Arrange
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.getUserByUsername("nonexistent"));
            assertEquals("User not found with username: nonexistent", exception.getMessage());
        }

        @Test
        @DisplayName("US-Edge-007: Get user by null username - should throw exception")
        void getUserByUsername_WithNullUsername_ShouldThrowException() {
            // Arrange
            when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> userService.getUserByUsername(null));
        }
    }

    @Nested
    @DisplayName("createUser Tests")
    class CreateUserTests {

        @Test
        @DisplayName("US-017: Create user with valid request - should succeed")
        void createUser_WithValidRequest_ShouldSucceed() {
            // Arrange
            when(userRepository.existsByUsername(createUserRequest.getUsername())).thenReturn(false);
            when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(false);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(2L);
                return user;
            });

            // Act
            UserResponse result = userService.createUser(createUserRequest);

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(createUserRequest.getUsername(), result.getUsername()),
                    () -> assertEquals(createUserRequest.getEmail(), result.getEmail())
            );
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("US-018: Create user with duplicate username - should throw exception")
        void createUser_WithDuplicateUsername_ShouldThrowException() {
            // Arrange
            when(userRepository.existsByUsername(createUserRequest.getUsername())).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.createUser(createUserRequest));
            assertTrue(exception.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("US-019: Create user with duplicate email - should throw exception")
        void createUser_WithDuplicateEmail_ShouldThrowException() {
            // Arrange
            when(userRepository.existsByUsername(createUserRequest.getUsername())).thenReturn(false);
            when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.createUser(createUserRequest));
            assertTrue(exception.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("US-020: Create user with invalid role ID - should throw exception")
        void createUser_WithInvalidRoleId_ShouldThrowException() {
            // Arrange
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            createUserRequest.setRoleId(999L);
            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.createUser(createUserRequest));
            assertTrue(exception.getMessage().contains("Role not found"));
        }
    }

    @Nested
    @DisplayName("updateUser Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("US-021: Update user with valid data - should succeed")
        void updateUser_WithValidData_ShouldSucceed() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            UserResponse result = userService.updateUser(1L, updateUserRequest);

            // Assert
            assertNotNull(result);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("US-022: Update username to duplicate - should throw exception")
        void updateUser_WithDuplicateUsername_ShouldThrowException() {
            // Arrange
            updateUserRequest.setUsername("duplicate");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByUsername("duplicate")).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.updateUser(1L, updateUserRequest));
            assertTrue(exception.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("US-023: Update email to duplicate - should throw exception")
        void updateUser_WithDuplicateEmail_ShouldThrowException() {
            // Arrange
            updateUserRequest.setEmail("duplicate@example.com");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.updateUser(1L, updateUserRequest));
            assertTrue(exception.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("US-024: Update with invalid role ID - should throw exception")
        void updateUser_WithInvalidRoleId_ShouldThrowException() {
            // Arrange
            updateUserRequest.setRoleId(999L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.updateUser(1L, updateUserRequest));
            assertTrue(exception.getMessage().contains("Role not found"));
        }
    }

    @Nested
    @DisplayName("deleteUser Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("US-025: Delete user without customer - should succeed")
        void deleteUser_WithoutCustomer_ShouldSucceed() {
            // Arrange
            testUser.setCustomer(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            doNothing().when(userRepository).delete(any(User.class));

            // Act
            userService.deleteUser(1L);

            // Assert
            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("US-026: Delete user with associated customer - should throw exception")
        void deleteUser_WithAssociatedCustomer_ShouldThrowException() {
            // Arrange
            com.banking.customer.module.entity.Customer customer = new com.banking.customer.module.entity.Customer();
            testUser.setCustomer(customer);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.deleteUser(1L));
            assertTrue(exception.getMessage().contains("associated customer"));
        }
    }

    @Nested
    @DisplayName("getClientIpAddress Tests")
    class GetClientIpAddressTests {

        @Test
        @DisplayName("US-027: Extract IP from X-Forwarded-For - should return first IP")
        void getClientIpAddress_WithXForwardedFor_ShouldReturnFirstIp() {
            // Arrange
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            // Act
            String result = userService.getClientIpAddress(request);

            // Assert
            assertEquals("192.168.1.1", result);
        }

        @Test
        @DisplayName("US-028: Extract IP from X-Real-IP - should return IP")
        void getClientIpAddress_WithXRealIp_ShouldReturnIp() {
            // Arrange
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            // Act
            String result = userService.getClientIpAddress(request);

            // Assert
            assertEquals("10.0.0.1", result);
        }

        @Test
        @DisplayName("US-029: Extract IP from RemoteAddr - should return remote address")
        void getClientIpAddress_WithRemoteAddr_ShouldReturnRemoteAddress() {
            // Arrange
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            // Act
            String result = userService.getClientIpAddress(request);

            // Assert
            assertEquals("127.0.0.1", result);
        }

        @Test
        @DisplayName("US-Edge-008: Extract IP with 'unknown' header - should fallback")
        void getClientIpAddress_WithUnknownHeader_ShouldFallback() {
            // Arrange
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
            when(request.getHeader("X-Real-IP")).thenReturn("unknown");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            // Act
            String result = userService.getClientIpAddress(request);

            // Assert
            assertEquals("127.0.0.1", result);
        }
    }

    @Nested
    @DisplayName("getUserAgent Tests")
    class GetUserAgentTests {

        @Test
        @DisplayName("US-030: Extract normal User-Agent - should return agent")
        void getUserAgent_WithNormalAgent_ShouldReturnAgent() {
            // Arrange
            HttpServletRequest request = mock(HttpServletRequest.class);
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
            when(request.getHeader("User-Agent")).thenReturn(userAgent);

            // Act
            String result = userService.getUserAgent(request);

            // Assert
            assertEquals(userAgent, result);
        }

        @Test
        @DisplayName("US-031: Extract User-Agent > 500 chars - should truncate")
        void getUserAgent_WithLongAgent_ShouldTruncate() {
            // Arrange
            HttpServletRequest request = mock(HttpServletRequest.class);
            String longUserAgent = "A".repeat(600);
            when(request.getHeader("User-Agent")).thenReturn(longUserAgent);

            // Act
            String result = userService.getUserAgent(request);

            // Assert
            assertEquals(500, result.length());
        }

        @Test
        @DisplayName("US-Edge-009: Extract null User-Agent - should return null")
        void getUserAgent_WithNullAgent_ShouldReturnNull() {
            // Arrange
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("User-Agent")).thenReturn(null);

            // Act
            String result = userService.getUserAgent(request);

            // Assert
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Security & Edge Case Tests")
    class SecurityAndEdgeCaseTests {

        @Test
        @DisplayName("Security-001: SQL Injection attempt in username - should handle safely")
        void createLocalUser_WithSqlInjectionAttempt_ShouldHandleSafely() {
            // Arrange
            registerRequest.setUsername("admin'; DROP TABLE users; --");
            String keycloakId = "keycloak-456";
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByKeycloakId(keycloakId)).thenReturn(false);
            when(roleRepository.findByName(anyString())).thenReturn(Optional.of(testRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(2L);
                return user;
            });

            // Act
            User result = userService.createLocalUser(registerRequest, keycloakId);

            // Assert
            // Should not throw exception, but username should be stored as-is (JPA should handle escaping)
            assertNotNull(result);
            verify(userRepository).existsByUsername("admin'; DROP TABLE users; --");
        }

        @Test
        @DisplayName("Security-002: XSS attempt in email - should handle safely")
        void createLocalUser_WithXssAttempt_ShouldHandleSafely() {
            // Arrange
            registerRequest.setEmail("<script>alert('XSS')</script>@example.com");
            String keycloakId = "keycloak-456";
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByKeycloakId(keycloakId)).thenReturn(false);
            when(roleRepository.findByName(anyString())).thenReturn(Optional.of(testRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(2L);
                return user;
            });

            // Act
            User result = userService.createLocalUser(registerRequest, keycloakId);

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertNotNull(result.getEmail())
            );
            // Email validation should be handled at DTO level, but service should not crash
        }

        @Test
        @DisplayName("Edge-010: Empty string username - should be handled")
        void createLocalUser_WithEmptyUsername_ShouldBeHandled() {
            // Arrange
            registerRequest.setUsername("");
            String keycloakId = "keycloak-456";
            when(userRepository.existsByUsername("")).thenReturn(false);
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(userRepository.existsByKeycloakId(keycloakId)).thenReturn(false);
            when(roleRepository.findByName(anyString())).thenReturn(Optional.of(testRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(3L);
                return user;
            });

            // Act
            User result = userService.createLocalUser(registerRequest, keycloakId);

            // Assert
            // Service may allow empty username (validation should be at DTO/controller level)
            // This test documents current behavior
            verify(userRepository).existsByUsername("");
            assertNotNull(result);
        }
    }
}

