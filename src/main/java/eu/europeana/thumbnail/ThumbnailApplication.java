package eu.europeana.thumbnail;


import org.apache.logging.log4j.LogManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Main application
 *
 * @author Srishti Singh
 * Created on 12-08-2019
 */
@SpringBootApplication
@PropertySource(value = "classpath:build.properties", ignoreResourceNotFound = true)
public class ThumbnailApplication extends SpringBootServletInitializer {

    private static final long maxAge=1000;
    /**
     * Setup CORS for all requests
     * @return WebMvcConfigurer that exposes CORS headers
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*").maxAge(maxAge)
                        .exposedHeaders("Allow, Vary, ETag, Last-Modified");
            }
        };
    }

    /**
     * This method is called when starting as a Spring-Boot application (e.g. from your IDE)
     * @param args main application paramaters
     */
    @SuppressWarnings("squid:S2095") // to avoid sonarqube false positive (see https://stackoverflow.com/a/37073154/741249)
    public static void main(String[] args) {
        LogManager.getLogger(ThumbnailApplication.class).info("CF_INSTANCE_INDEX  = {}, CF_INSTANCE_GUID = {}, CF_INSTANCE_IP  = {}",
                System.getenv("CF_INSTANCE_INDEX"), System.getenv("CF_INSTANCE_GUID"), System.getenv("CF_INSTANCE_IP"));
        SpringApplication.run(ThumbnailApplication.class, args);
    }

}
