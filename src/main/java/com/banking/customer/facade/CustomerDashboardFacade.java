package com.banking.customer.facade;

import com.banking.customer.dto.CustomerDashboardDto;

/**
 * Customer Dashboard Facade
 * 
 * Provides a simplified interface for aggregating dashboard data from multiple services.
 * Follows the Facade design pattern to hide the complexity of fetching and assembling
 * data from Account, Transaction, and Notification services.
 */
public interface CustomerDashboardFacade {
    /**
     * Get comprehensive dashboard data for a customer
     * 
     * @param username The username of the customer
     * @return CustomerDashboardDto containing aggregated dashboard data
     * @throws com.banking.core.exception.UserNotFoundException if user is not found
     * @throws com.banking.core.exception.CustomerNotFoundException if customer is not found
     */
    CustomerDashboardDto getCustomerDashboard(String username);
}

