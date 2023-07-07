package com.example.loadbalancer;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class Scheduler {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Scheduler.class)
                .properties("instantiate-once=true", "server.port=8082")
                .run(args);
    }
}
