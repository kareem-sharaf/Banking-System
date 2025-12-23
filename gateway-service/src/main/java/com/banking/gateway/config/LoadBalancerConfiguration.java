package com.banking.gateway.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

@Configuration
public class LoadBalancerConfiguration {

    private static final String SERVICE_ID = "banking-service";

    /**
     * Static list of instances. Replace hosts/ports to match your deployment
     * targets.
     */
    @Bean
    @Primary
    public ServiceInstanceListSupplier serviceInstanceListSupplier() {
        List<ServiceInstance> instances = Arrays.asList(
                new DefaultServiceInstance(SERVICE_ID + "-1", SERVICE_ID, "localhost", 8081, false),
                new DefaultServiceInstance(SERVICE_ID + "-2", SERVICE_ID, "localhost", 8082, false),
                new DefaultServiceInstance(SERVICE_ID + "-3", SERVICE_ID, "localhost", 8083, false));

        return new ServiceInstanceListSupplier() {
            @Override
            public String getServiceId() {
                return SERVICE_ID;
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                return Flux.just(instances);
            }
        };
    }

    /**
     * Use round-robin across the static instances.
     */
    @Bean
    public ReactorServiceInstanceLoadBalancer reactorServiceInstanceLoadBalancer(
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {
        return new RoundRobinLoadBalancer(
                serviceInstanceListSupplierProvider,
                SERVICE_ID);
    }
}
