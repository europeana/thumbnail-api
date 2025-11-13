package eu.europeana.thumbnail.config;

import com.amazonaws.ClientConfiguration;
import eu.europeana.features.S3ObjectStorageClient;
import eu.europeana.thumbnail.exception.ConfigurationException;
import eu.europeana.thumbnail.service.MediaReadStorageService;
import eu.europeana.thumbnail.service.impl.IiifImageReadServerImpl;
import eu.europeana.thumbnail.service.impl.LogoUploadService;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Loads all routes and storages from configuration.
 *
 * @author Patrick Ehlert
 * Created on 1 sep 2020
 */
@Configuration
@PropertySource(value = "classpath:thumbnail.properties")
@PropertySource(value = "classpath:thumbnail.user.properties", ignoreResourceNotFound = true)
public class StorageRoutes {

    private static final Logger LOG = LogManager.getLogger(StorageRoutes.class);

    private static final String PROP_ROUTE         = "route";
    private static final String PROP_ROUTE_NAME    = "name";
    private static final String PROP_ROUTE_STORAGE = "storage";

    private static final String PROP_S3_KEY        = "s3.key";
    private static final String PROP_S3_SECRET     = "s3.secret";
    private static final String PROP_S3_REGION     = "s3.region";
    private static final String PROP_S3_BUCKET     = "s3.bucket";
    private static final String PROP_S3_ENDPOINT   = "s3.endpoint";

    private static final String PROP_VALIDATE_CONNECTION_AFTER = "s3.validate.connection";
    private static final String PROP_MAX_CONNECTIONS = "s3.max.connections";
    private static final String PROPERTY_SEPARATOR = ".";
    private static final String VALUE_SEPARATOR    = ",";

    private static final String PROP_LOGO_UPLOAD_STORAGE = "logos.storage";

    private String defaultRoute;

    // The list of MediaStorageServices has to be an ordered list, so we can guarantee proper order of retrieval!
    private Map<String, List<MediaReadStorageService>> routeToStorages = new HashMap<>();
    private Map<String, MediaReadStorageService> storageNameToService = new HashMap<>();

    private String logoUploadStorageName;

    private Environment environment;

    /**
     * Initialize configuration of routes and corresponding media storages.
     * @param environment Spring-Boot environment to load the configuration from
     */
    public StorageRoutes(Environment environment) {
        this.environment = environment;
    }

    /**
     * Load all routes and storages from configuration and put them in the routeToStorage map
     */
    @PostConstruct
    private void initRoutesToStorage() {
        String uploadStorageName = environment.getProperty(PROP_LOGO_UPLOAD_STORAGE);
        if (!StringUtils.isBlank(uploadStorageName)) {
            this.logoUploadStorageName = uploadStorageName.trim();
            LOG.info("Configured logo upload storage = {}", this.logoUploadStorageName);
        }

        int i = 1;
        String routeKeyNr = PROP_ROUTE + i;
        String routeKeyName = routeKeyNr + PROPERTY_SEPARATOR + PROP_ROUTE_NAME;

        while (environment.containsProperty(routeKeyName)) {
            // get routes
            String[] routes = environment.getProperty(routeKeyName).split(VALUE_SEPARATOR);
            // set first loaded route as default
            if  (defaultRoute == null) {
                defaultRoute = routes[0].trim();
            }

            // get storage names
            String routePropStorage = routeKeyNr + PROPERTY_SEPARATOR + PROP_ROUTE_STORAGE;
            if (environment.containsProperty(routePropStorage)) {
                String[] storages = environment.getProperty(routePropStorage).split(VALUE_SEPARATOR);
                for (String route : routes) {
                    // trim to remove spaces
                    String cleanRoute = route.trim();
                    LOG.info("Adding route {} with storage(s) {}", cleanRoute, storages);
                    routeToStorages.put(cleanRoute, generateStorageServices(storages, this.logoUploadStorageName));
                }
            } else {
                throw new ConfigurationException(("No storage defined for route(s)" + routes));
            }

            i++;
            routeKeyNr = PROP_ROUTE + i;
            routeKeyName = routeKeyNr + PROPERTY_SEPARATOR + PROP_ROUTE_NAME;
        }

        if (routeToStorages.isEmpty()) {
            throw new ConfigurationException("No routes and storages configured!");
        }

    }

    private ArrayList<MediaReadStorageService> generateStorageServices(String[] storageNames, String uploadStorageName) {
        ArrayList<MediaReadStorageService> result = new ArrayList<>();
        for (String storageName : storageNames) {
            // trim values to prevent trailing space
            String name = storageName.trim();

            // check if we already created this storage before
            MediaReadStorageService service = storageNameToService.get(name);
            if (service == null) {
                service = createNewService(name, uploadStorageName);
            } else {
                LOG.info("Reusing existing service {}", service.getName());
            }
            storageNameToService.put(name, service);
            result.add(service);
        }
        return result;
    }

    private MediaReadStorageService createNewService(String storageName, String logoUploadStorageName) {
        LOG.info("Setting up new client {}...", storageName);
        if (storageName.equalsIgnoreCase(IiifImageReadServerImpl.STORAGE_NAME)) {
            LOG.info("Creating IIIF Image Server client...");
            return new IiifImageReadServerImpl();
        }

        String key = environment.getRequiredProperty(storageName + PROPERTY_SEPARATOR + PROP_S3_KEY);
        String secret = environment.getRequiredProperty(storageName + PROPERTY_SEPARATOR + PROP_S3_SECRET);
        String region = environment.getRequiredProperty(storageName + PROPERTY_SEPARATOR + PROP_S3_REGION);
        String bucket = environment.getRequiredProperty(storageName + PROPERTY_SEPARATOR + PROP_S3_BUCKET);
        String endpoint = environment.getProperty(storageName + PROPERTY_SEPARATOR + PROP_S3_ENDPOINT);
        Integer maxConnections = environment.getProperty(storageName + PROPERTY_SEPARATOR + PROP_MAX_CONNECTIONS, Integer.class, 50);
        Integer validateAfter = environment.getProperty(storageName + PROPERTY_SEPARATOR + PROP_VALIDATE_CONNECTION_AFTER, Integer.class, -1);
        ClientConfiguration config = new ClientConfiguration();
        if (maxConnections > 1) {
            config.setMaxConnections(maxConnections);
            LOG.info("Configured maximum connections = {}", config.getMaxConnections());
        }
        if (validateAfter >= 100) {
            config.withValidateAfterInactivityMillis(validateAfter);
            LOG.info("Configured validating connection after = {} ms", config.getValidateAfterInactivityMillis());
        }
        
        if (StringUtils.isEmpty(endpoint)) {
            LOG.info("Creating Amazon storage client {}...", storageName);
            return new eu.europeana.thumbnail.service.impl.MediaReadStorageServiceImpl(storageName, 
                    new S3ObjectStorageClient(key, secret, region, bucket, config));
        }
        if (storageName.equalsIgnoreCase(logoUploadStorageName)) {
            LOG.info("Creating IBM read/write storage client {}...", storageName);
            return new LogoUploadService(storageName,
                    new S3ObjectStorageClient(key, secret, region, bucket, endpoint, config));
        }
        LOG.info("Creating IBM read storage client {}...", storageName);
        return new eu.europeana.thumbnail.service.impl.MediaReadStorageServiceImpl(storageName,
                new S3ObjectStorageClient(key, secret, region, bucket, endpoint, config));
    }

    /**
     * Returns the first loaded route as a default (in case there is no match with other routes).
     * @return String containing the default route
     */
    public String getDefaultRoute() {
        return defaultRoute;
    }

    /**
     * LogoUploadService that uses the appropriate S3 client for uploading images
     * @return service, or null if nothing was configured
     */
    public LogoUploadService getLogoUploadService() {
        return (LogoUploadService) storageNameToService.get(this.logoUploadStorageName);
    }

    /**
     * Returns a map of route names (top-level FQDN) and a list of storages services, ordered by priority.
     * @return Map of route names and ordered media storage service
     */
    public Map<String, List<MediaReadStorageService>> getRoutesMap() {
        return routeToStorages;
    }

}
