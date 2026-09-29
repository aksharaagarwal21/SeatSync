package com.seatsync.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** A real PostgreSQL: row locks, version checks and partial unique indexes behave exactly as in production. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withCommand("postgres", "-c", "max_connections=200");
    }

    /** Replaces email delivery so tests can read the codes they need. */
    @Bean
    @Primary
    RecordingOtpNotifier recordingOtpNotifier() {
        return new RecordingOtpNotifier();
    }
}
