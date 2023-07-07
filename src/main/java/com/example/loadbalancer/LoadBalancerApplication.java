package com.example.loadbalancer;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class LoadBalancerApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(LoadBalancerApplication.class)
                .properties("instantiate-once=false", "server.port=8080")
                .run(args);
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }
}
