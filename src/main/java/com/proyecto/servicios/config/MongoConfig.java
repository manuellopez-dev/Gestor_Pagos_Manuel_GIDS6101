package com.proyecto.servicios.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.proyecto.servicios.repositorys.producto")
public class MongoConfig {
}