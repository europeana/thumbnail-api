package eu.europeana.thumbnail.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Basic Thumbnail API configuration
 */
@Configuration
@PropertySource(value = "classpath:thumbnail.properties")
@PropertySource(value = "classpath:thumbnail.user.properties", ignoreResourceNotFound = true)
public class ApiConfig {

    private static final Logger LOG = LogManager.getLogger(ApiConfig.class);

    private static final String BEAN_CLIENT_DETAILS_SERVICE ="commons_oauth2_europeanaClientDetailsService";
    private static final String BEAN_I18N_SERVICE = "i18nService";
    private static final String BEAN_I18N_MESSAGE_SOURCE = "messageSource";

    @Value("${upload.auth.enabled:true}")
    private boolean uploadAuthEnabled;

    // TODO tmp property until we have token validation for SB3
    @Value("${upload.auth.wskey}")
    private String wskey;

    @Value("${keycloak.token.endpoint:}")
    private String tokenEndpoint;

    @Value("${keycloak.token.grant.params:}")
    private String grantParams;

    /**
     * @return true if the application is configured to require authorization for uploading logo's, otherwise false
     */
    public boolean isUploadAuthEnabled() {
        return uploadAuthEnabled;
    }

    public String getWskey() {
        return wskey;
    }

//    @Bean(name = BEAN_CLIENT_DETAILS_SERVICE)
//    public EuropeanaClientDetailsService getApiKeyClientDetailsService(){
//        EuropeanaClientDetailsService clientDetails = new EuropeanaClientDetailsService();
//        if (StringUtils.isNotEmpty(tokenEndpoint) && StringUtils.isNotEmpty(grantParams)) {
//            AuthenticationConfig config = new AuthenticationConfig(tokenEndpoint, grantParams);
//            clientDetails.setAuthHandler(AuthenticationBuilder.newAuthentication(config));
//        }else{
//            LOG.warn("Keycloak token endpoint and/or grant parameters are NOT set !! ");
//        }
//        return clientDetails;
//    }

//    @Bean(name = BEAN_I18N_MESSAGE_SOURCE)
//    public MessageSource i18nMessagesSource(){
//        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
//        source.setBasename("classpath:messages");
//        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
//        return source;
//    }
//
//    @Bean(name = BEAN_I18N_SERVICE)
//    public I18nService getI18nService() {
//        return new I18nServiceImpl(i18nMessagesSource());
//    }
}
