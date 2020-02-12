package eu.europeana.thumbnail;

import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.ELRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main application
 *
 * @author Srishti Singh
 * Created on 12-08-2019
 */
@SpringBootApplication
@PropertySource(value = "classpath:build.properties", ignoreResourceNotFound = true)
@PropertySource("classpath:thumbnail.properties")
@PropertySource(value = "classpath:thumbnail.user.properties", ignoreResourceNotFound = true)
public class ThumbnailApplication extends SpringBootServletInitializer {

    @Value("${features.security.enable}")
    private boolean securityEnable;

    @Value("${security.config.ipRanges}")
    private String ipRanges;

    /**
     * This method is called when starting as a Spring-Boot application (e.g. from your IDE)
     *
     * @param args main application paramaters
     */
    @SuppressWarnings("squid:S2095")
    // to avoid sonarqube false positive (see https://stackoverflow.com/a/37073154/741249)
    public static void main(String[] args) {
        LogManager.getLogger(ThumbnailApplication.class)
                  .info("CF_INSTANCE_INDEX  = {}, CF_INSTANCE_GUID = {}, CF_INSTANCE_IP  = {}",
                        System.getenv("CF_INSTANCE_INDEX"),
                        System.getenv("CF_INSTANCE_GUID"),
                        System.getenv("CF_INSTANCE_IP"));
        SpringApplication.run(ThumbnailApplication.class, args);
    }

    /**
     * Setup CORS for all requests
     *
     * @return WebMvcConfigurer that exposes CORS headers
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .exposedHeaders("Allow", "ETag", "Cache-Control", "Last-Modified");
            }
        };
    }

    @EnableWebSecurity
    @Configuration
    class SecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            ELRequestMatcher matcher = new ELRequestMatcher("hasHeader('user-agent', 'runscope-radar/2.0')" );

            if (securityEnable) {
                http.authorizeRequests()
                        .antMatchers("/api/**", "/thumbnail/**")
                        .access(createHasIpRangeExpression())
                        .requestMatchers(matcher).permitAll();
            }
        }

//        @Override
//        public void configure(WebSecurity web) throws Exception {
//            web.ignoring().antMatchers("**/runscope.com/radar/**");
//        }
        /**
         * creates the string for authorizing request for the provided ipRanges
         */
        private String createHasIpRangeExpression() {
            List<String> validIps = Arrays.asList(ipRanges.split("\\s*,\\s*"));
            return validIps.stream()
                    .collect(Collectors.joining("') or hasIpAddress('", "hasIpAddress('", "')"));
        }

        //https://api.runscope.com/radar/bucket/3a0a035c-23e9-4208-81e6-37448dfcec07/trigger?environment=thumbnail-test.eanadev.org

        @Bean
        public DelegatingAuthenticationEntryPoint delegatingAuthenticationEntryPoint() {

            ELRequestMatcher matcher = new ELRequestMatcher("hasHeader('user-agent', 'runscope-radar/2.0')" );

            LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> map =
                    new LinkedHashMap<RequestMatcher, AuthenticationEntryPoint>();
            map.put(matcher, new BasicAuthenticationEntryPoint());

            DelegatingAuthenticationEntryPoint delegatingAuthenticationEntryPoint = new DelegatingAuthenticationEntryPoint(map);
            delegatingAuthenticationEntryPoint.setDefaultEntryPoint( new Http403ForbiddenEntryPoint());


            return delegatingAuthenticationEntryPoint;
        }
    }
}
