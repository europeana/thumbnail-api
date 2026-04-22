package eu.europeana.thumbnail.health;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStateManager {

    private final ApplicationEventPublisher eventPublisher;

    public ApplicationStateManager(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    // Call this when your app finishes initializing
    public void markReady() {
        AvailabilityChangeEvent.publish(
                eventPublisher,
                this,
                ReadinessState.ACCEPTING_TRAFFIC
        );
    }

    // Call this during graceful shutdown
    public void markNotReady() {
        AvailabilityChangeEvent.publish(
                eventPublisher,
                this,
                ReadinessState.REFUSING_TRAFFIC
        );
    }

    // Call this if your app enters an unrecoverable state
    public void markBroken() {
        AvailabilityChangeEvent.publish(
                eventPublisher,
                this,
                LivenessState.BROKEN
        );
    }
}
