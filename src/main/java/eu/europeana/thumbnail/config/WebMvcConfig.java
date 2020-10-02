package eu.europeana.thumbnail.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Setup CORS for all requests
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Setup CORS for all requests.
     * Note that this doesn't work for the Swagger endpoint
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedHeaders("*")
                .allowedMethods(HttpMethod.GET.name(),
                                HttpMethod.HEAD.name(),
                                HttpMethod.OPTIONS.name())
                .exposedHeaders(HttpHeaders.ALLOW,
                                HttpHeaders.CACHE_CONTROL,
                                HttpHeaders.ETAG,
                                HttpHeaders.LAST_MODIFIED)
                .maxAge(1000L); // in seconds
    }

}
