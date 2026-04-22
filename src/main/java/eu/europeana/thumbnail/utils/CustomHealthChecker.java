package eu.europeana.thumbnail.utils;

import eu.europeana.thumbnail.config.StorageRoutes;
import eu.europeana.thumbnail.service.MediaReadStorageService;
import eu.europeana.thumbnail.service.impl.UploadImageServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import eu.europeana.thumbnail.service.impl.MediaReadStorageServiceImpl;
import software.amazon.awssdk.services.s3.model.Bucket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CustomHealthChecker implements HealthIndicator {

    private static final Logger LOG = LogManager.getLogger(CustomHealthChecker.class);
    private final Map<String, MediaReadStorageService> storageNameToService;
    private final Map<String, String> serviceBucketMap;

    public CustomHealthChecker(StorageRoutes storageRoutes) {
        this.storageNameToService = storageRoutes.getMediaStorageServices();
        this.serviceBucketMap = storageRoutes.getServiceBucketMap();
    }

    @Override
    public Health health() {
        try {
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
                            LOG.debug("{} / {} is available", entry.getKey(), entry.getValue());
                            bucketUp = true;
                            break;
                        }
                    }
                    if (!bucketUp) {
                        LOG.error("{} / {} is not available", entry.getKey(), entry.getValue());
                    }
                    serviceBucketHealthMap.put(entry.getKey() + "/" + entry.getValue(), bucketUp);
                }
            }
            if (serviceBucketHealthMap.containsValue(false)){
                return Health.down().withDetail("S3 thumbnail buckets", "one or more not found").build();
            } else {
                return Health.up().withDetail("S3 thumbnail buckets", "OK").build();
            }
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}



