package eu.europeana.thumbnail.config;

import com.amazonaws.ClientConfiguration;
import eu.europeana.features.S3ObjectStorageClient;
import eu.europeana.thumbnail.exception.ConfigurationException;
import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.thumbnail.service.impl.IiifImageServerImpl;
import eu.europeana.thumbnail.service.impl.MediaStorageServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
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

    private static final String PROPERTY_SEPARATOR = ".";
    private static final String VALUE_SEPARATOR    = ",";

    private String defaultRoute;

    // The list of MediaStorageServices has to be an ordered list, so we can guarantee proper order of retrieval!
    private Map<String, List<MediaStorageService>> routeToStorages = new HashMap<>();
    private Map<String, MediaStorageService> storageNameToService = new HashMap<>();

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
                    routeToStorages.put(cleanRoute, generateStorageServices(storages));
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

    private ArrayList<MediaStorageService> generateStorageServices(String[] storageNames) {
        ArrayList<MediaStorageService> result = new ArrayList<>();
        for (String storageName : storageNames) {
            // trim values to prevent trailing space
            String name = storageName.trim();

            // check if we already created this storage before
            MediaStorageService service = storageNameToService.get(name);
            if (service == null) {
                service = createNewService(name);
            } else {
                LOG.info("Reusing existing service {}", service.getName());
            }
            storageNameToService.put(name, service);
            result.add(service);
        }
        return result;
    }

    private MediaStorageService createNewService(String storageName) {
        if (storageName.equalsIgnoreCase(IiifImageServerImpl.STORAGE_NAME)) {
            LOG.debug("Creating IIIF Image Server client {}...", storageName);
            return new IiifImageServerImpl();
        }

        String key = environment.getRequiredProperty(storageName + PROPERTY_SEPARATOR + PROP_S3_KEY);
        String secret = environment.getRequiredProperty(storageName + PROPERTY_SEPARATOR + PROP_S3_SECRET);
        String region = environment.getRequiredProperty(storageName + PROPERTY_SEPARATOR + PROP_S3_REGION);
        String bucket = environment.getRequiredProperty(storageName + PROPERTY_SEPARATOR + PROP_S3_BUCKET);
        String endpoint = environment.getProperty(storageName + PROPERTY_SEPARATOR + PROP_S3_ENDPOINT);
        if (StringUtils.isEmpty(endpoint)) {
            LOG.debug("Creating Amazon storage client {}...", storageName);
            // 14 aug 2024 we disabled the validateAfterInactivity to check if it's still required
            //ClientConfiguration config = new ClientConfiguration().withValidateAfterInactivityMillis(2000);
            return new MediaStorageServiceImpl(storageName, new S3ObjectStorageClient(key, secret, region, bucket)); //, config));
        }
        LOG.debug("Creating IBM storage client {}...", storageName);
        return new MediaStorageServiceImpl(storageName, new S3ObjectStorageClient(key, secret, region, bucket, endpoint));
    }

    /**
     * Returns the first loaded route as a default (in case there is no match with other routes).
     * @return String containing the default route
     */
    public String getDefaultRoute() {
        return defaultRoute;
    }

    /**
     * Returns a map of route names (top-level FQDN) and a list of storages services, ordered by priority.
     * @return Map of route names and ordered media storage service
     */
    public Map<String, List<MediaStorageService>> getRoutesMap() {
        return routeToStorages;
    }

}
