package eu.europeana.thumbnail.health;

import eu.europeana.thumbnail.config.StorageRoutes;
import eu.europeana.thumbnail.service.MediaReadStorageService;
import eu.europeana.thumbnail.service.impl.UploadImageServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import eu.europeana.thumbnail.service.impl.MediaReadStorageServiceImpl;
import software.amazon.awssdk.services.s3.model.Bucket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            Map<String, Boolean> serviceBucketHealthMap = new HashMap<>();
            for (var entry : serviceBucketMap.entrySet()) {
                boolean bucketUp = false;
                List<Bucket> bucketList = new ArrayList<>();
                if (storageNameToService.containsKey(entry.getKey())){
                    if (storageNameToService.get(entry.getKey()) instanceof MediaReadStorageServiceImpl mss){
                        bucketList = mss.getObjectStorageClient().listBuckets();
                    } else if (storageNameToService.get(entry.getKey()) instanceof UploadImageServiceImpl uis){
                        bucketList = uis.getObjectStorageClient().listBuckets();
                    }
                    for (Bucket bucket : bucketList){
                        if (bucket.name().equalsIgnoreCase(entry.getValue())){
                            LOG.info("{} / {} is available", entry.getKey(), entry.getValue());
                            bucketUp = true;
                            break;
                        }
                    }
                    if (!bucketUp) {
                        nrDown++;
                        if (nrDown == 1){
                            sb.append("s");
                        } else if (nrDown > 1){
                            sb.append(",");
                        }
                        sb.append(" ").append(entry.getKey()).append("/").append(entry.getValue());

                        System.out.println(entry.getKey() + " / " + entry.getValue() + " is not available");
                        LOG.error("{} / {} is not available", entry.getKey(), entry.getValue());
                    }
                    serviceBucketHealthMap.put(entry.getKey() + "/" + entry.getValue(), bucketUp);
                }
            }
            sb.append(nrDown > 1 ? " is" : " are").append(" not available");
            if (serviceBucketHealthMap.containsValue(false)){
                LOG.error(sb);
                return Health.down().withDetail("S3 Thumbnail health", sb.toString()).build();
            } else {
                return Health.up().withDetail("S3 Thumbnail health", "OK").build();
            }
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}