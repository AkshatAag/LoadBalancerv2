package com.example.loadbalancer;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@SpringBootApplication
public class LoadBalancerInstanceApplication2 {

    public static void main(String[] args) {
        new SpringApplicationBuilder(LoadBalancerInstanceApplication2.class)
                .properties("instantiate-once=false","server.port=8081")
                .run(args);
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }
}
