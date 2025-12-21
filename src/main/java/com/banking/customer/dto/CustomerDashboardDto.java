package com.banking.customer.facade.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CustomerDashboardDto {
    private String customerName;
    private String customerNumber;
    private String customerStatus;
    private List<AccountSummaryDto> accounts;
    private List<TransactionSummaryDto> recentTransactions;
    private List<NotificationSummaryDto> notifications;
}
