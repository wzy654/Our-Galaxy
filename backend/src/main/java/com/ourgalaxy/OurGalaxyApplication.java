package com.ourgalaxy;

import org.springframework.boot.SpringApplication;
import com.ourgalaxy.common.security.JwtProperties;
import com.ourgalaxy.file.StorageProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, StorageProperties.class})
public class OurGalaxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(OurGalaxyApplication.class, args);
    }
}
