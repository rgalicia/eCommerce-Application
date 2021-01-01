package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@EnableJpaRepositories("com.example.demo.model.persistence.repositories") // Avoid using @Repository annotation?
@EntityScan("com.example.demo.model.persistence") // Just looks here for @Entity / @Embeddable classes
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class}) // we're doing a JWT implementation
public class ECommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ECommerceApplication.class, args);
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
