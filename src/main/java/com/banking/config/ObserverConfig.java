package com.banking.config;

import com.banking.account.service.notification.AccountObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Observer Configuration
 * 
 * Configuration class for Observer Pattern setup.
 * Spring automatically collects all beans implementing AccountObserver
 * and injects them as a List<AccountObserver>.
 * 
 * This configuration can be extended to:
 * - Enable/disable specific observers
 * - Set observer priorities
 * - Configure observer-specific settings
 * 
 * @author Banking System
 */
@Configuration
@Slf4j
public class ObserverConfig {

    /**
     * Log all registered observers at startup
     * 
     * Spring will automatically inject all AccountObserver implementations
     * into this method when the application starts.
     * 
     * @param observers List of all AccountObserver implementations
     * @return The same list (for chaining if needed)
     */
    @Bean
    public List<AccountObserver> accountObservers(List<AccountObserver> observers) {
        log.info("=== Observer Pattern Configuration ===");
        log.info("Registered {} account observer(s):", observers.size());

        observers.forEach(
                observer -> log.info("  - {} ({})", observer.getClass().getSimpleName(), observer.getObserverType()));

        log.info("=======================================");

        return observers;
    }
}
