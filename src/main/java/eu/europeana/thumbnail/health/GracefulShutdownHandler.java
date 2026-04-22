package eu.europeana.thumbnail.health;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class GracefulShutdownHandler implements ApplicationListener<ContextClosedEvent> {

    private static final Logger LOG = LogManager.getLogger(GracefulShutdownHandler.class);
    private final ApplicationStateManager stateManager;

    public GracefulShutdownHandler(ApplicationStateManager stateManager) {
        this.stateManager = stateManager;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        // Mark as not ready - Kubernetes will stop sending traffic
        LOG.info("Graceful shutdown of Thumbnail API in progress");
//        System.out.println("Graceful shutdown of Thumbnail API in progress");
        stateManager.markNotReady();

        try {
            // Wait for in-flight requests to complete
            // This should match your terminationGracePeriodSeconds
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
