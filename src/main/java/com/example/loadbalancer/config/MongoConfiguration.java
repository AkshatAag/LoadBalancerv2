package com.example.loadbalancer.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories
public class MongoConfiguration extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Override
    protected String getDatabaseName() {
        return "user";
    }

    @Override
    public MongoClient mongoClient() {
        System.out.println("mongoclient was given out");
        return MongoClients.create(mongoUri);
    }

    @Override
    protected boolean autoIndexCreation() {
        System.out.println("indexes were created " );

        return true; // Enable automatic index creation
    }
}

