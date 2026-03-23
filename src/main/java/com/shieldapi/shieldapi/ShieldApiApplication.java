package com.shieldapi.shieldapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ShieldApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShieldApiApplication.class, args);
    }
}
