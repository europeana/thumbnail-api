package eu.europeana.thumbnail.health;

import jakarta.annotation.Nonnull;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Component
public class StartupApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    private final ApplicationStateManager stateManager;

    public StartupApplicationListener(ApplicationStateManager stateManager) {
        this.stateManager = stateManager;
    }

    private static final Logger LOG = LogManager.getLogger(StartupApplicationListener.class);


    @Override public void onApplicationEvent(@Nonnull ContextRefreshedEvent event) {
        LOG.info("Thumbnail API initialised");
//        System.out.println("Thumbnail API initialised");
        // Mark as not ready - Kubernetes will stop sending traffic
        stateManager.markReady();
    }

}
