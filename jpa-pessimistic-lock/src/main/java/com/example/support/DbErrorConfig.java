package com.example.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DbErrorConfig {

    @Bean
    public DbErrorClassifier dbErrorClassifier(DataSource dataSource) {
        return new DbErrorClassifier(dataSource);
    }
}
