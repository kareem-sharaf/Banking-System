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
public class DirectorApprovalHandler extends ApprovalHandler {

    @Value("${transaction.approval.director.threshold:10000}")
    private BigDecimal minThreshold;

    @Override
    protected boolean canHandle(ApprovalContext context) {
        if (context == null || context.getAmount() == null) {
            return false;
        }

        // Can handle if amount is greater than or equal to threshold
        boolean canHandle = context.getAmount().compareTo(minThreshold) >= 0;

        log.debug("DirectorApprovalHandler.canHandle: amount={}, threshold={}, canHandle={}",
                context.getAmount(), minThreshold, canHandle);

        return canHandle;
    }

    @Override
    protected ApprovalResult process(ApprovalContext context) {
        log.info("Transaction {} requires director approval (amount: {})",
                context.getTransaction().getTransactionNumber(),
                context.getAmount());

        return ApprovalResult.builder()
                .status(ApprovalStatus.PENDING)
                .transaction(context.getTransaction())
                .message(String.format("Transaction requires director approval (amount: %s >= threshold: %s)",
                        context.getAmount(), minThreshold))
                .comment("Pending director approval")
                .build();
    }

    @Override
    public String getHandlerName() {
        return "DirectorApprovalHandler";
    }

    public BigDecimal getMinThreshold() {
        return minThreshold;
    }
}
