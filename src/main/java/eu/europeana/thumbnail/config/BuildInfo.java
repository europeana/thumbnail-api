package eu.europeana.thumbnail.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Makes the information from the project's pom.xml available. While generating a war file this data is written
 * automatically to the build.properties file which is read here.
 */
@Configuration
@PropertySource("classpath:build.properties")
public class BuildInfo {

    @Value("${info.app.name}")
    private String appName;

    @Value("${info.app.version}")
    private String appVersion;

    @Value("${info.app.description}")
    private String appDescription;

    @Value("${info.build.number}")
    private String buildNumber;

    public String getAppName() {
        return appName;
    }

    public String getAppDescription() {
        return appDescription;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getBuildNumber() {
        return buildNumber;
    }
}
