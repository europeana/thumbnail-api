package eu.europeana.thumbnail.service;

import eu.europeana.thumbnail.config.StorageRoutes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * This class will return a list of storage services to load a thumbnail from, given a FQDN or top-level name in a
 * request. The list of storage services is ordered so the first storage should be checked first if it contains the
 * thumbnail. If not, we check the second, etc.
 *
 * @author Patrick Ehlert
 * Created on 21 sep 2020
 */
@Service
public class StoragesService {

    private static final Logger LOG = LogManager.getLogger(StoragesService.class);

    private StorageRoutes storageRoutes;

    public StoragesService(StorageRoutes storageRoutes) {
        this.storageRoutes = storageRoutes;
    }

    /**
     * Given a route (request hostname) return a list of storages to retrieve the thumbnail from
     * @param route the highest level domain name or FQDN
     * @return list of MediaStorageService
     */
    public List<MediaStorageService> getStorages(String route) {
        // make sure we use only the highest level part for matching and not the FQDN
        String topLevelName = getTopLevelName(route);

        // exact matching
        List<MediaStorageService> result = storageRoutes.getRoutesMap().get(topLevelName);
        if (result != null) {
            LOG.debug("Route {} - found exact match", topLevelName);
            return result;
        }

        // fallback 1: try to match with "contains"
        for (Map.Entry<String, List<MediaStorageService>> entry : storageRoutes.getRoutesMap().entrySet()) {
            if (topLevelName.contains(entry.getKey())) {
                LOG.debug("Route {} - matched with {}", topLevelName, entry.getKey());
                return entry.getValue();
            }
        }

        // fallback 2: use default, but log warning
        LOG.warn("Route {} - no configured storage found, using default", topLevelName);
        return storageRoutes.getRoutesMap().get(storageRoutes.getDefaultRoute());
    }

    private String getTopLevelName(String route) {
        int i = route.indexOf('.');
        System.out.println(i);
        if (i >= 0) {
            return route.substring(0, i);
        }
        return route;
    }
}
