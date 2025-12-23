package com.banking.customer.service;

import com.banking.core.auth.module.entity.User;
import com.banking.core.enums.CustomerStatus;
import com.banking.customer.dto.CreateCustomerRequest;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.dto.UpdateCustomerRequest;
import com.banking.customer.module.entity.Customer;
import com.banking.customer.repository.CustomerRepository;
import com.banking.core.auth.repository.UserRepository;
import com.banking.support.module.entity.SupportTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Tests for CustomerService
 * 
 * Test Strategy: AAA Pattern (Arrange-Act-Assert)
 * Coverage: >95% line coverage, 100% critical path coverage
 * 
 * @author QA Automation Team
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer testCustomer;
    private User testUser;
    private CreateCustomerRequest createCustomerRequest;
    private UpdateCustomerRequest updateCustomerRequest;

    @BeforeEach
    void setUp() {
        // Arrange: Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        // Arrange: Create test customer
        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setUser(testUser);
        testCustomer.setCustomerNumber("CUST001");
        testCustomer.setStatus(CustomerStatus.ACTIVE);
        testCustomer.setJoinDate(LocalDateTime.now());
        testCustomer.setCreatedAt(LocalDateTime.now());
        testCustomer.setAccounts(new ArrayList<>());
        testCustomer.setSupportTickets(new ArrayList<>());

        // Arrange: Create request DTOs
        createCustomerRequest = CreateCustomerRequest.builder()
                .userId(1L)
                .status(CustomerStatus.ACTIVE)
                .build();

        updateCustomerRequest = new UpdateCustomerRequest();
        updateCustomerRequest.setStatus(CustomerStatus.SUSPENDED);
    }

    @Nested
    @DisplayName("createCustomer Tests")
    class CreateCustomerTests {

        @Test
        @DisplayName("CS-001: Create customer with valid user - should succeed")
        void createCustomer_WithValidUser_ShouldSucceed() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(customerRepository.existsByUserId(1L)).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
                Customer customer = invocation.getArgument(0);
                customer.setId(2L);
                customer.setCustomerNumber("CUST002");
                return customer;
            });

            // Act
            CustomerResponse result = customerService.createCustomer(createCustomerRequest);

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(1L, result.getUserId()),
                    () -> assertNotNull(result.getCustomerNumber()),
                    () -> assertEquals(CustomerStatus.ACTIVE, result.getStatus())
            );
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("CS-002: Create customer with invalid user ID - should throw exception")
        void createCustomer_WithInvalidUserId_ShouldThrowException() {
            // Arrange
            createCustomerRequest.setUserId(999L);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> customerService.createCustomer(createCustomerRequest));
            assertTrue(exception.getMessage().contains("User not found"));
        }

        @Test
        @DisplayName("CS-003: Create customer when already exists - should throw exception")
        void createCustomer_WhenAlreadyExists_ShouldThrowException() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(customerRepository.existsByUserId(1L)).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> customerService.createCustomer(createCustomerRequest));
            assertTrue(exception.getMessage().contains("Customer already exists"));
        }

        @Test
        @DisplayName("CS-004: Create customer with default status (ACTIVE) - should set status")
        void createCustomer_WithDefaultStatus_ShouldSetStatus() {
            // Arrange
            createCustomerRequest.setStatus(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(customerRepository.existsByUserId(1L)).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
                Customer customer = invocation.getArgument(0);
                customer.setId(2L);
                customer.setCustomerNumber("CUST002");
                return customer;
            });

            // Act
            CustomerResponse result = customerService.createCustomer(createCustomerRequest);

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(CustomerStatus.ACTIVE, result.getStatus())
            );
            ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(customerCaptor.capture());
            Customer savedCustomer = customerCaptor.getValue();
            assertEquals(CustomerStatus.ACTIVE, savedCustomer.getStatus());
        }
    }

    @Nested
    @DisplayName("getAllCustomers Tests")
    class GetAllCustomersTests {

        @Test
        @DisplayName("CS-005: Retrieve all customers - should return list")
        void getAllCustomers_WithCustomers_ShouldReturnList() {
            // Arrange
            List<Customer> customers = List.of(testCustomer);
            when(customerRepository.findAll()).thenReturn(customers);

            // Act
            List<CustomerResponse> result = customerService.getAllCustomers();

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(testCustomer.getCustomerNumber(), result.get(0).getCustomerNumber())
            );
        }

        @Test
        @DisplayName("CS-Edge-001: Retrieve all customers when empty - should return empty list")
        void getAllCustomers_WithNoCustomers_ShouldReturnEmptyList() {
            // Arrange
            when(customerRepository.findAll()).thenReturn(new ArrayList<>());

            // Act
            List<CustomerResponse> result = customerService.getAllCustomers();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getCustomerById Tests")
    class GetCustomerByIdTests {

        @Test
        @DisplayName("CS-006: Get customer by valid ID - should return customer")
        void getCustomerById_WithValidId_ShouldReturnCustomer() {
            // Arrange
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

            // Act
            CustomerResponse result = customerService.getCustomerById(1L);

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(1L, result.getId()),
                    () -> assertEquals(testCustomer.getCustomerNumber(), result.getCustomerNumber())
            );
        }

        @Test
        @DisplayName("CS-007: Get customer by invalid ID - should throw exception")
        void getCustomerById_WithInvalidId_ShouldThrowException() {
            // Arrange
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> customerService.getCustomerById(999L));
            assertTrue(exception.getMessage().contains("Customer not found"));
        }
    }

    @Nested
    @DisplayName("getCustomerByUserId Tests")
    class GetCustomerByUserIdTests {

        @Test
        @DisplayName("CS-008: Get customer by valid user ID - should return customer")
        void getCustomerByUserId_WithValidUserId_ShouldReturnCustomer() {
            // Arrange
            when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));

            // Act
            CustomerResponse result = customerService.getCustomerByUserId(1L);

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(1L, result.getUserId())
            );
        }

        @Test
        @DisplayName("CS-009: Get customer by invalid user ID - should throw exception")
        void getCustomerByUserId_WithInvalidUserId_ShouldThrowException() {
            // Arrange
            when(customerRepository.findByUserId(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> customerService.getCustomerByUserId(999L));
            assertTrue(exception.getMessage().contains("Customer not found"));
        }
    }

    @Nested
    @DisplayName("getCustomerByCustomerNumber Tests")
    class GetCustomerByCustomerNumberTests {

        @Test
        @DisplayName("CS-010: Get customer by valid customer number - should return customer")
        void getCustomerByCustomerNumber_WithValidNumber_ShouldReturnCustomer() {
            // Arrange
            when(customerRepository.findByCustomerNumber("CUST001"))
                    .thenReturn(Optional.of(testCustomer));

            // Act
            CustomerResponse result = customerService.getCustomerByCustomerNumber("CUST001");

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals("CUST001", result.getCustomerNumber())
            );
        }

        @Test
        @DisplayName("CS-011: Get customer by invalid customer number - should throw exception")
        void getCustomerByCustomerNumber_WithInvalidNumber_ShouldThrowException() {
            // Arrange
            when(customerRepository.findByCustomerNumber("INVALID")).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> customerService.getCustomerByCustomerNumber("INVALID"));
            assertTrue(exception.getMessage().contains("Customer not found"));
        }
    }

    @Nested
    @DisplayName("getCustomersByStatus Tests")
    class GetCustomersByStatusTests {

        @Test
        @DisplayName("CS-012: Get customers by status - should return filtered list")
        void getCustomersByStatus_WithStatus_ShouldReturnFilteredList() {
            // Arrange
            List<Customer> customers = List.of(testCustomer);
            when(customerRepository.findByStatus(CustomerStatus.ACTIVE)).thenReturn(customers);

            // Act
            List<CustomerResponse> result = customerService.getCustomersByStatus(CustomerStatus.ACTIVE);

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(CustomerStatus.ACTIVE, result.get(0).getStatus())
            );
        }
    }

    @Nested
    @DisplayName("updateCustomer Tests")
    class UpdateCustomerTests {

        @Test
        @DisplayName("CS-013: Update customer status - should update customer")
        void updateCustomer_WithStatusUpdate_ShouldUpdateCustomer() {
            // Arrange
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

            // Act
            CustomerResponse result = customerService.updateCustomer(1L, updateCustomerRequest);

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(CustomerStatus.SUSPENDED, result.getStatus())
            );
            ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(customerCaptor.capture());
            assertEquals(CustomerStatus.SUSPENDED, customerCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("CS-014: Update non-existent customer - should throw exception")
        void updateCustomer_WithNonExistentCustomer_ShouldThrowException() {
            // Arrange
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> customerService.updateCustomer(999L, updateCustomerRequest));
            assertTrue(exception.getMessage().contains("Customer not found"));
        }
    }

    @Nested
    @DisplayName("deleteCustomer Tests")
    class DeleteCustomerTests {

        @Test
        @DisplayName("CS-015: Delete customer without accounts - should succeed")
        void deleteCustomer_WithoutAccounts_ShouldSucceed() {
            // Arrange
            testCustomer.setAccounts(new ArrayList<>());
            testCustomer.setSupportTickets(new ArrayList<>());
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            doNothing().when(customerRepository).delete(any(Customer.class));

            // Act
            customerService.deleteCustomer(1L);

            // Assert
            verify(customerRepository).delete(testCustomer);
        }

        @Test
        @DisplayName("CS-016: Delete customer with accounts - should throw exception")
        void deleteCustomer_WithAccounts_ShouldThrowException() {
            // Arrange
            List<com.banking.account.module.entity.Account> accounts = new ArrayList<>();
            accounts.add(new com.banking.account.module.entity.Account());
            testCustomer.setAccounts(accounts);
            testCustomer.setSupportTickets(new ArrayList<>());

            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> customerService.deleteCustomer(1L));
            assertTrue(exception.getMessage().contains("associated accounts"));
        }

        @Test
        @DisplayName("CS-017: Delete customer with support tickets - should throw exception")
        void deleteCustomer_WithSupportTickets_ShouldThrowException() {
            // Arrange
            List<SupportTicket> tickets = new ArrayList<>();
            SupportTicket ticket = new SupportTicket();
            tickets.add(ticket);
            
            Customer customerWithTickets = new Customer();
            customerWithTickets.setId(1L);
            customerWithTickets.setAccounts(new ArrayList<>());
            customerWithTickets.setSupportTickets(tickets);
            
            when(customerRepository.findById(1L)).thenReturn(Optional.of(customerWithTickets));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> customerService.deleteCustomer(1L));
            assertTrue(exception.getMessage().contains("associated support tickets"));
        }

        @Test
        @DisplayName("CS-018: Delete non-existent customer - should throw exception")
        void deleteCustomer_WithNonExistentCustomer_ShouldThrowException() {
            // Arrange
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> customerService.deleteCustomer(999L));
            assertTrue(exception.getMessage().contains("Customer not found"));
        }
    }

    @Nested
    @DisplayName("existsByUserId Tests")
    class ExistsByUserIdTests {

        @Test
        @DisplayName("CS-Edge-002: Check if customer exists by user ID - should return boolean")
        void existsByUserId_WithValidUserId_ShouldReturnBoolean() {
            // Arrange
            when(customerRepository.existsByUserId(1L)).thenReturn(true);

            // Act
            boolean exists = customerService.existsByUserId(1L);

            // Assert
            assertTrue(exists);
            verify(customerRepository).existsByUserId(1L);
        }
    }

    @Nested
    @DisplayName("existsByCustomerNumber Tests")
    class ExistsByCustomerNumberTests {

        @Test
        @DisplayName("CS-Edge-003: Check if customer exists by customer number - should return boolean")
        void existsByCustomerNumber_WithValidNumber_ShouldReturnBoolean() {
            // Arrange
            when(customerRepository.existsByCustomerNumber("CUST001")).thenReturn(true);

            // Act
            boolean exists = customerService.existsByCustomerNumber("CUST001");

            // Assert
            assertTrue(exists);
            verify(customerRepository).existsByCustomerNumber("CUST001");
        }
    }
}

