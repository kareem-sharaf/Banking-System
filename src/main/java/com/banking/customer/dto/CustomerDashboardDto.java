package com.banking.customer.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

import com.banking.account.dto.AccountSummaryDto;
import com.banking.core.notification.dto.NotificationSummaryDto;
import com.banking.transaction.dto.TransactionSummaryDto;

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
