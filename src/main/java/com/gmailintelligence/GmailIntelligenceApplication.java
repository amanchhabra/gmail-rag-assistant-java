package com.gmailintelligence;

import com.gmailintelligence.configuration.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class GmailIntelligenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmailIntelligenceApplication.class, args);
    }
}
