package eu.europeana.thumbnail.config;

import eu.europeana.api.commons_sb3.definitions.oauth.Role;
import eu.europeana.api.commons_sb3.error.exceptions.ApplicationAuthenticationException;
import eu.europeana.api.commons_sb3.error.i18n.I18nService;
import eu.europeana.api.commons_sb3.error.i18n.I18nServiceImpl;
import eu.europeana.api.commons_sb3.oauth2.service.authorization.BaseAuthorizationService;
import eu.europeana.api.commons_sb3.oauth2.service.impl.EuropeanaClientDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;

/**
 * Basic Thumbnail API configuration, including settings for authentication
 */
@Configuration
@PropertySource(value = "classpath:thumbnail.properties")
@PropertySource(value = "classpath:thumbnail.user.properties", ignoreResourceNotFound = true)
public class ApiConfig extends BaseAuthorizationService {

    private static final String BEAN_I18N_SERVICE = "i18nService";
    private static final String BEAN_I18N_MESSAGE_SOURCE = "messageSource";

    @Value("${upload.auth.enabled:true}")
    private String uploadAuthEnabled;  // Workaround to fix issue with unit test not being able to read boolean type

    @Value("${auth.api.name:}")
    private String authApiName;

    @Value("${auth.token.signature:}")
    private String authTokenSignature;

    /**
     * @return true if the application is configured to require authorization for uploading logo's, otherwise false
     */
    public boolean isUploadAuthEnabled() {
        // For some reason the junit tests fail when loading this property directly as boolean value using @Value annotation
        return Boolean.parseBoolean(uploadAuthEnabled);
    }

    @Override
    protected String getApiName() {
        if (this.authApiName == null) {
            return null;
        }
        return this.authApiName.trim(); // we trim to avoid issues with accidentally added spaces
    }

    @Override
    protected String getSignatureKey() {
        return this.authTokenSignature;
    }

    @Override
    protected Role getRoleByName(String s) {
        // not needed for our purposes
        return null;
    }

    /**
     * To only allow client credentials and not user credentials, this method should return false
     * @param operation not used
     * @return false always
     */
    @Override
    protected boolean isResourceAccessVerificationRequired(String operation) {
        return false;
    }

    /**
     * No need to implement since we only require checking client credentials and not user credentials
     * @return null always
     */
    public EuropeanaClientDetailsService getClientDetailsService(){
        return null;
    }

    @Override
    public Authentication authorizeWriteAccess(HttpServletRequest request, String operation) throws ApplicationAuthenticationException {
        if (isUploadAuthEnabled()) {
            return super.authorizeWriteAccess(request, operation);
        }
        return null;
    }

    @Bean(name = BEAN_I18N_MESSAGE_SOURCE)
    public MessageSource i18nMessagesSource(){
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return source;
    }

    @Bean(name = BEAN_I18N_SERVICE)
    public I18nService getI18nService() {
        return new I18nServiceImpl(i18nMessagesSource());
    }
}
