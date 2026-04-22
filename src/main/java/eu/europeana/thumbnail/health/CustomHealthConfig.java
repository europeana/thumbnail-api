package eu.europeana.thumbnail.health;

import org.springframework.boot.actuate.availability.LivenessStateHealthIndicator;
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomHealthConfig {

    // Liveness should be simple - is the JVM running?
    @Bean
    public LivenessStateHealthIndicator livenessIndicator(
            ApplicationAvailability availability) {
        return new LivenessStateHealthIndicator(availability);
    }

    // Readiness checks if we can handle traffic
    @Bean
    public ReadinessStateHealthIndicator readinessIndicator(
            ApplicationAvailability availability) {
        return new ReadinessStateHealthIndicator(availability);
    }
}