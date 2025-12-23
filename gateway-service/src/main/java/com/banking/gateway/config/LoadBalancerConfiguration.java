package com.banking.gateway.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties.SimpleServiceInstance;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoadBalancerConfiguration {

    private static final String SERVICE_ID = "banking-service";

    /**
     * Static list of instances. Replace hosts/ports to match your deployment
     * targets.
     */
    @Bean
    public ServiceInstanceListSupplier serviceInstanceListSupplier(ConfigurableApplicationContext context) {
        List<ServiceInstance> instances = Arrays.asList(
                new SimpleServiceInstance(SERVICE_ID + "-1", SERVICE_ID, "localhost", 8081, false),
                new SimpleServiceInstance(SERVICE_ID + "-2", SERVICE_ID, "localhost", 8082, false),
                new SimpleServiceInstance(SERVICE_ID + "-3", SERVICE_ID, "localhost", 8083, false));
        return ServiceInstanceListSuppliers.from(SERVICE_ID, instances).build(context);
    }

    /**
     * Use round-robin across the static instances.
     */
    @Bean
    public ReactorServiceInstanceLoadBalancer reactorServiceInstanceLoadBalancer(
            ServiceInstanceListSupplier supplier) {
        return new RoundRobinLoadBalancer(supplier, SERVICE_ID);
    }
}
