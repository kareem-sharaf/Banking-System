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
public class ManagerApprovalHandler extends ApprovalHandler {

    @Value("${transaction.approval.manager.min-threshold:1000}")
    private BigDecimal minThreshold;

    @Value("${transaction.approval.manager.max-threshold:10000}")
    private BigDecimal maxThreshold;

    @Override
    protected boolean canHandle(ApprovalContext context) {
        if (context == null || context.getAmount() == null) {
            return false;
        }

        BigDecimal amount = context.getAmount();

        // Can handle if amount is within manager's approval range
        boolean canHandle = (amount.compareTo(minThreshold) >= 0) && (amount.compareTo(maxThreshold) < 0);

        log.debug("ManagerApprovalHandler.canHandle: amount={}, minThreshold={}, maxThreshold={}, canHandle={}",
                amount, minThreshold, maxThreshold, canHandle);

        return canHandle;
    }

    @Override
    protected ApprovalResult process(ApprovalContext context) {
        log.info("Transaction {} requires manager approval (amount: {})",
                context.getTransaction().getTransactionNumber(),
                context.getAmount());

        return ApprovalResult.builder()
                .status(ApprovalStatus.PENDING)
                .transaction(context.getTransaction())
                .message(String.format("Transaction requires manager approval (amount: %s, range: %s - %s)",
                        context.getAmount(), minThreshold, maxThreshold))
                .comment("Pending manager approval")
                .build();
    }

    @Override
    public String getHandlerName() {
        return "ManagerApprovalHandler";
    }

    public BigDecimal getMinThreshold() {
        return minThreshold;
    }

    public BigDecimal getMaxThreshold() {
        return maxThreshold;
    }
}
