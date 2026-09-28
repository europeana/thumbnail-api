package eu.europeana.thumbnail.health;

import eu.europeana.thumbnail.config.StorageRoutes;
import eu.europeana.thumbnail.service.MediaReadStorageService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import eu.europeana.thumbnail.service.impl.MediaReadStorageServiceImpl;

import java.util.Map;

@Component("s3")
@ConditionalOnEnabledHealthIndicator("s3") // Enables/disables this indicator via properties
public class S3HealthChecker implements HealthIndicator {

    private static final Logger LOG = LogManager.getLogger(S3HealthChecker.class);
    private final Map<String, MediaReadStorageService> storageNameToService;
    private final Map<String, String> serviceBucketMap;

    // Cleaned up: No longer depends on ApplicationStateManager
    public S3HealthChecker(StorageRoutes storageRoutes) {
        this.storageNameToService = storageRoutes.getMediaStorageServices();
        this.serviceBucketMap = storageRoutes.getServiceBucketMap();
    }

    @Override
    public Health health() {
        try {
            int nrDown = 0;
            StringBuilder sb = new StringBuilder("Thumbnail API not healthy: bucket");
            for (var entry : serviceBucketMap.entrySet()) {
                if (storageNameToService.containsKey(entry.getKey())){
                    boolean bucketUp = isBucketAvailable(storageNameToService.get(entry.getKey()), entry.getValue());
                    if (!bucketUp) {
                        nrDown++;
                        if (nrDown == 1){
                            sb.append("s");
                        } else if (nrDown > 1){
                            sb.append(",");
                        }
                        sb.append(" ").append(entry.getKey()).append("/").append(entry.getValue());
                        LOG.error("{} / {} is not available", entry.getKey(), entry.getValue());
                    }
                }
            }
            sb.append(nrDown > 1 ? " is" : " are").append(" not available");
            if (nrDown > 0){
                LOG.error(sb);
                return Health.down().withDetail("S3 Thumbnail health", sb.toString()).build();
            } else {
                return Health.up().withDetail("S3 Thumbnail health", "OK").build();
            }
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }

    private boolean isBucketAvailable(MediaReadStorageService service, String bucketName) {
        if (service instanceof MediaReadStorageServiceImpl mss) {
            return mss.getObjectStorageClient().listBuckets().stream()
                    .anyMatch(bucket -> bucket.name().equalsIgnoreCase(bucketName));
        }
        return false;
    }
}