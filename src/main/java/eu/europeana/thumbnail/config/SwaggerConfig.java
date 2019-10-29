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
 * Configures swagger on all requests. Swagger Json file is availabe at <hostname>/v2/api-docs and if you add
 * Swagger UI package the <hostname>/swagger-ui.html
 *
 * @author Srishti Singh
 * Created on 12-08-2019
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).select()
                                                      .apis(RequestHandlerSelectors.basePackage("eu.europeana.thumbnail"))
                                                      .paths(PathSelectors.any())
                                                      .build();
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private ApiInfo apiInfo() {
        return new ApiInfo("Europeana-Thumbnail-API",
                           "Generate thumbnail",
                           null,
                           null,
                           new Contact("APIs team", "www.europeana.eu", "api@europeana.eu"),
                           "EUPL 1.2",
                           "API license URL",
                           Collections.emptyList());
    }
}
