package eu.europeana.thumbnail.health;

import eu.europeana.thumbnail.config.StorageRoutes;
import eu.europeana.thumbnail.service.MediaReadStorageService;
import eu.europeana.thumbnail.service.impl.MediaReadStorageServiceImpl;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import static eu.europeana.thumbnail.config.StorageRoutes.PRIMARY_STORAGE;

@Component(PRIMARY_STORAGE)
@ConditionalOnEnabledHealthIndicator(PRIMARY_STORAGE)
public class PrimaryStorageHealthIndicator implements HealthIndicator {

    private final StorageRoutes storageRoutes;

    public PrimaryStorageHealthIndicator(StorageRoutes storageRoutes) {
        this.storageRoutes = storageRoutes;
    }

    @Override
    public Health health() {
        try {
            MediaReadStorageService primaryStorage =
                    storageRoutes.getPrimaryStorage();

            String bucketName = storageRoutes.getServiceBucketMap()
                    .get(primaryStorage.getName());

            if (bucketName == null) {
                return Health.down()
                        .withDetail(PRIMARY_STORAGE,
                                "No bucket configured for primary storage: "
                                        + primaryStorage.getName())
                        .build();
            }

            if (!(primaryStorage instanceof MediaReadStorageServiceImpl s3Storage)) {
                return Health.down()
                        .withDetail(PRIMARY_STORAGE,
                                "Primary storage is not an S3 storage: "
                                        + primaryStorage.getName())
                        .build();
            }

            boolean bucketAvailable = s3Storage.getObjectStorageClient()
                    .listBuckets()
                    .stream()
                    .anyMatch(bucket ->
                            bucket.name().equalsIgnoreCase(bucketName));

            if (bucketAvailable) {
                return Health.up()
                        .withDetail(PRIMARY_STORAGE, "OK")
                        .build();
            }

            return Health.down()
                    .withDetail(PRIMARY_STORAGE,
                            "Primary bucket is not available: " + bucketName)
                    .build();

        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}