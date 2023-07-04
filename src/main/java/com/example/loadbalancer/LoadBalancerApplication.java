package com.example.loadbalancer;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class LoadBalancerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoadBalancerApplication.class, args);
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }
}
