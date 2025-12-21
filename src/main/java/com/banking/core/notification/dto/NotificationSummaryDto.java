package com.banking.customer.facade.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationSummaryDto {
    private String title;
    private String message;
    private String priority;
    private LocalDateTime date;
    private boolean read;
}
