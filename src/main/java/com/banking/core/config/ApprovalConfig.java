package com.banking.core.config;

import com.banking.transaction.approval.ApprovalChainBuilder;
import com.banking.transaction.approval.ApprovalHandler;
import com.banking.transaction.approval.handler.AutoApprovalHandler;
import com.banking.transaction.approval.handler.DirectorApprovalHandler;
import com.banking.transaction.approval.handler.ManagerApprovalHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Approval Configuration
 * 
 * Configures the approval chain as a Spring Bean.
 * The chain is built at application startup and can be modified at runtime.
 */
@Configuration
@Slf4j
public class ApprovalConfig {

        /**
         * Build and configure the approval chain
         *
         * The chain is built in the following order:
         * 1. AutoApprovalHandler - for small transactions (< threshold)
         * 2. ManagerApprovalHandler - for medium transactions (threshold -
         * maxThreshold)
         * 3. DirectorApprovalHandler - for large transactions (>= maxThreshold)
         *
         * @param autoHandler     Auto approval handler
         * @param managerHandler  Manager approval handler
         * @param directorHandler Director approval handler
         * @return The first handler in the approval chain
         */
        @Bean
        @Primary
        public ApprovalHandler approvalHandlerChain(
                        AutoApprovalHandler autoHandler,
                        ManagerApprovalHandler managerHandler,
                        DirectorApprovalHandler directorHandler) {

                log.info("=== Building Approval Chain ===");
                log.info("Auto Approval Handler: {} (threshold: {})",
                                autoHandler.getHandlerName(), autoHandler.getThreshold());
                log.info("Manager Approval Handler: {} (range: {} - {})",
                                managerHandler.getHandlerName(),
                                managerHandler.getMinThreshold(),
                                managerHandler.getMaxThreshold());
                log.info("Director Approval Handler: {} (threshold: {})",
                                directorHandler.getHandlerName(), directorHandler.getMinThreshold());
                log.info("=================================");

                // Build the default chain
                ApprovalHandler chain = ApprovalChainBuilder.buildDefaultChain(
                                autoHandler,
                                managerHandler,
                                directorHandler);

                log.info("Approval chain built successfully. Starting handler: {}", chain.getHandlerName());

                return chain;
        }
}
