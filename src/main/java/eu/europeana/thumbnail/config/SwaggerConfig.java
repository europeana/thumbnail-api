package eu.europeana.thumbnail.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

/**
 * Configures Swagger on all requests. Swagger Json file is availabe at <hostname>/v2/api-docs and at
 * <hostname/v3/api-docs. Swagger UI is available at <hostname>/swagger-ui/
 *
 * @author Srishti Singh
 * Created on 12-08-2019
 * @author Patrick Ehlert (major refactoring in sep 2020)
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    private BuildInfo buildInfo;

    /**
     * Initialize Swagger with API build information
     * @param buildInfo object for retrieving build information
     */
    public SwaggerConfig(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

    /**
     * Initialize Swagger Documentation
     * @return Swagger Docket for this API
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("eu.europeana.thumbnail"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                buildInfo.getAppName(),
                buildInfo.getAppDescription(),
                buildInfo.getAppVersion() + "(build " + buildInfo.getBuildNumber() + ")",
                null,
                new Contact("API team", "https://api.europeana.eu", "api@europeana.eu"),
                "EUPL 1.2", "https://www.eupl.eu", Collections.emptyList());
    }
}
