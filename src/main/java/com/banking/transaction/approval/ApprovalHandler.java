package com.banking.transaction.approval;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ApprovalHandler {

    protected ApprovalHandler next;

    public void setNext(ApprovalHandler next) {
        this.next = next;
    }

    public final ApprovalResult handle(ApprovalContext context) {
        log.debug("Handler {} checking if it can handle transaction {}",
                getHandlerName(),
                context.getTransaction() != null ? context.getTransaction().getTransactionNumber() : "null");

        // Check if this handler can handle the request
        if (canHandle(context)) {
            log.debug("Handler {} is processing transaction {}",
                    getHandlerName(),
                    context.getTransaction().getTransactionNumber());

            ApprovalResult result = process(context);
            result.setHandlerUsed(getHandlerName());
            return result;
        }

        // Pass to next handler if available
        if (next != null) {
            log.debug("Handler {} cannot handle, passing to next handler", getHandlerName());
            return next.handle(context);
        }

        // No handler could process the request
        log.warn("No handler in the chain could process the transaction: {}",
                context.getTransaction() != null ? context.getTransaction().getTransactionNumber() : "null");

        return ApprovalResult.builder()
                .status(com.banking.core.enums.ApprovalStatus.REJECTED)
                .message("No suitable handler found for this transaction")
                .handlerUsed("None")
                .transaction(context.getTransaction())
                .build();
    }

    protected abstract boolean canHandle(ApprovalContext context);

    protected abstract ApprovalResult process(ApprovalContext context);

    public abstract String getHandlerName();
}
