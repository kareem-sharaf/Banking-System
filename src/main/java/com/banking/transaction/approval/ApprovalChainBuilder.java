package com.banking.transaction.approval;

import lombok.extern.slf4j.Slf4j;

/**
 * Approval Chain Builder
 * 
 * Builder class for constructing the approval chain dynamically.
 * Supports fluent API for building chains at runtime.
 */
@Slf4j
public class ApprovalChainBuilder {

    private ApprovalHandler firstHandler;
    private ApprovalHandler currentHandler;

    /**
     * Add a handler to the chain
     * 
     * @param handler The handler to add
     * @return This builder for method chaining
     */
    public ApprovalChainBuilder addHandler(ApprovalHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }

        if (firstHandler == null) {
            // First handler in the chain
            firstHandler = handler;
            currentHandler = handler;
        } else {
            // Link the new handler to the current one
            currentHandler.setNext(handler);
            currentHandler = handler;
        }

        log.debug("Added handler {} to approval chain", handler.getHandlerName());
        return this;
    }

    /**
     * Build and return the chain
     * 
     * @return The first handler in the chain
     */
    public ApprovalHandler build() {
        if (firstHandler == null) {
            throw new IllegalStateException("Cannot build chain: no handlers added");
        }

        log.info("Built approval chain starting with handler: {}", firstHandler.getHandlerName());
        return firstHandler;
    }

    /**
     * Build the default approval chain
     * 
     * Order: AutoApprovalHandler -> ManagerApprovalHandler ->
     * DirectorApprovalHandler
     * 
     * @param autoHandler     Auto approval handler
     * @param managerHandler  Manager approval handler
     * @param directorHandler Director approval handler
     * @return The first handler in the chain
     */
    public static ApprovalHandler buildDefaultChain(
            ApprovalHandler autoHandler,
            ApprovalHandler managerHandler,
            ApprovalHandler directorHandler) {

        ApprovalChainBuilder builder = new ApprovalChainBuilder();

        // Build chain in order: Auto -> Manager -> Director
        if (autoHandler != null) {
            builder.addHandler(autoHandler);
        }
        if (managerHandler != null) {
            builder.addHandler(managerHandler);
        }
        if (directorHandler != null) {
            builder.addHandler(directorHandler);
        }

        return builder.build();
    }

    /**
     * Reset the builder for building a new chain
     */
    public ApprovalChainBuilder reset() {
        firstHandler = null;
        currentHandler = null;
        return this;
    }
}
