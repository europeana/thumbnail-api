package eu.europeana.thumbnail.config;

import eu.europeana.features.S3ObjectStorageClient;
import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.thumbnail.exception.ConfigurationException;
import eu.europeana.thumbnail.service.impl.IiifImageServerImpl;
import eu.europeana.thumbnail.service.impl.MediaStorageServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
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
    private Map<String, ArrayList<MediaStorageService>> routeToStorageService = new HashMap<>();
    private Map<String, MediaStorageService> storageNameToService = new HashMap<>();

    private Environment environment;

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
                    LOG.info("Adding route {} with storage(s) {}", route.trim(), storages);
                    routeToStorageService.put(route.trim(), generateStorageServices(storages));
                }
            } else {
                throw new ConfigurationException(("No storage defined for route(s)" + routes));
            }

            i++;
            routeKeyNr = PROP_ROUTE + i;
            routeKeyName = routeKeyNr + PROPERTY_SEPARATOR + PROP_ROUTE_NAME;
        }

        if (routeToStorageService.isEmpty()) {
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
            return new MediaStorageServiceImpl(storageName, new S3ObjectStorageClient(key, secret, region, bucket));
        }
        LOG.debug("Creating IBM storage client {}...", storageName);
        return new MediaStorageServiceImpl(storageName, new S3ObjectStorageClient(key, secret, region, bucket, endpoint));
    }

    /**
     * Given a route (request hostname) return a list of storages to retrieve the thumbnail from
     * @param route the highest level domain name or FQDN
     * @return list of MediaStorageService
     */
    public List<MediaStorageService> getStorageService(String route) {
        // make sure we use only the highest level part for matching and not the FQDN
        String topLevelName = getTopLevelName(route);

        // exact matching
        List<MediaStorageService> result = routeToStorageService.get(topLevelName);
        if (result != null) {
            LOG.debug("Route {} - found exact match", topLevelName);
            return result;
        }

        // fallback 1: try to match with "contains"
        for (Map.Entry<String, ArrayList<MediaStorageService>> entry : routeToStorageService.entrySet()) {
            if (topLevelName.contains(entry.getKey())) {
                LOG.debug("Route {} - matched with {}", topLevelName, entry.getKey());
                return entry.getValue();
            }
        }

        // fallback 2: use default, but log warning
        LOG.warn("Route {} - no configured storage found, using default", topLevelName);
        return routeToStorageService.get(defaultRoute);
    }

    private String getTopLevelName(String route) {
        int i = route.indexOf('.');
        if (i >= 0) {
            return route.substring(0, i);
        }
        return route;
    }
}
