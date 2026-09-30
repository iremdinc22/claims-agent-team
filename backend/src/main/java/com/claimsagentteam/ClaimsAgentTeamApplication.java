package com.claimsagentteam;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ClaimsAgentTeamApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimsAgentTeamApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

}
