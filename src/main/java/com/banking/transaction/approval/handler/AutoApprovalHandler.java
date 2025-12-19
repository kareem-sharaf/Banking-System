package com.banking.transaction.approval.handler;

import com.banking.core.enums.ApprovalStatus;
import com.banking.transaction.approval.ApprovalContext;
import com.banking.transaction.approval.ApprovalHandler;
import com.banking.transaction.approval.ApprovalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Slf4j
public class AutoApprovalHandler extends ApprovalHandler {

    @Value("${transaction.approval.auto.threshold:10000}")
    private BigDecimal threshold;

    @Override
    protected boolean canHandle(ApprovalContext context) {
        if (context == null || context.getAmount() == null) {
            return false;
        }

        // Can handle if amount is less than threshold
        boolean canHandle = context.getAmount().compareTo(threshold) < 0;

        log.debug("AutoApprovalHandler.canHandle: amount={}, threshold={}, canHandle={}",
                context.getAmount(), threshold, canHandle);

        return canHandle;
    }

    @Override
    protected ApprovalResult process(ApprovalContext context) {
        log.info("Auto-approving transaction {} with amount {}",
                context.getTransaction().getTransactionNumber(),
                context.getAmount());

        return ApprovalResult.builder()
                .status(ApprovalStatus.AUTO_APPROVED)
                .transaction(context.getTransaction())
                .approvedAt(LocalDateTime.now())
                .message(String.format("Transaction auto-approved (amount: %s < threshold: %s)",
                        context.getAmount(), threshold))
                .comment("Automatically approved - below threshold")
                .build();
    }

    @Override
    public String getHandlerName() {
        return "AutoApprovalHandler";
    }

    public BigDecimal getThreshold() {
        return threshold;
    }
}
