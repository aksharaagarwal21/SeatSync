package com.seatsync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Event dates and times are venue-local; the configured zone is used to interpret them. */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock(@Value("${seatsync.time-zone}") ZoneId zone) {
        return Clock.system(zone);
    }
}
