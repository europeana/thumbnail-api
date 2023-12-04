package eu.europeana.thumbnail;

import org.apache.logging.log4j.LogManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Main application
 *
 * @author Srishti Singh
 * Created on 12-08-2019
 */
@SpringBootApplication(scanBasePackages = {"eu.europeana.api", "eu.europeana.thumbnail"})
public class ThumbnailApplication extends SpringBootServletInitializer {

    /**
     * This method is called when starting as a Spring-Boot application (e.g. from your IDE)
     *
     * @param args main application paramaters
     */
    public static void main(String[] args) {
        // When deploying to Cloud Foundry, this will log the instance index number, IP and GUID
        LogManager.getLogger(ThumbnailApplication.class)
                  .info("CF_INSTANCE_INDEX  = {}, CF_INSTANCE_GUID = {}, CF_INSTANCE_IP  = {}",
                        System.getenv("CF_INSTANCE_INDEX"),
                        System.getenv("CF_INSTANCE_GUID"),
                        System.getenv("CF_INSTANCE_IP"));

        SpringApplication.run(ThumbnailApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ThumbnailApplication.class);
    }

}
