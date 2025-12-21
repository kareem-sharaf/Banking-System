package com.banking.customer.facade;

import com.banking.customer.facade.dto.CustomerDashboardDto;

public interface CustomerDashboardFacade {
    CustomerDashboardDto getCustomerDashboard(String username);
}
