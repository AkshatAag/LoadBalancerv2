package com.example.loadbalancer;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PreDestroy;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class SchedulerApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(SchedulerApplication.class)
                .properties("instantiate-once=true", "server.port=8082")
                .run(args);
    }
}
