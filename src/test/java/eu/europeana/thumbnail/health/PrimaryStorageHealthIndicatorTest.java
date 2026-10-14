
package eu.europeana.thumbnail.health;

import eu.europeana.thumbnail.config.StorageRoutes;
import eu.europeana.thumbnail.service.MediaReadStorageService;
import eu.europeana.thumbnail.service.impl.MediaReadStorageServiceImpl;
import eu.europeana.s3.S3ObjectStorageClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.boot.actuate.health.Health;
import software.amazon.awssdk.services.s3.model.Bucket;

class PrimaryStorageHealthIndicatorTest {

    private static final String PRIMARY_STORAGE_NAME = "metis-prod";
    private static final String PRIMARY_BUCKET_NAME = "thumbnail-bucket";

    @Test
    void primaryStorageIsUpWhenBucketIsAvailable() {
        StorageRoutes routes = mock(StorageRoutes.class);
        S3ObjectStorageClient s3Client = mock(S3ObjectStorageClient.class);
        MediaReadStorageServiceImpl primaryStorage =
                new MediaReadStorageServiceImpl(PRIMARY_STORAGE_NAME, s3Client);

        when(routes.getPrimaryStorage()).thenReturn(primaryStorage);
        when(routes.getServiceBucketMap())
                .thenReturn(Map.of(PRIMARY_STORAGE_NAME, PRIMARY_BUCKET_NAME));

        when(s3Client.listBuckets())
                .thenReturn(List.of(
                        Bucket.builder().name(PRIMARY_BUCKET_NAME).build()
                ));

        PrimaryStorageHealthIndicator indicator =  new PrimaryStorageHealthIndicator(routes);
        Health health = indicator.health();
        assertEquals("UP", health.getStatus().getCode());
    }


    @Test
    void primaryStorageIsDownWhenBucketIsMissing() {
        StorageRoutes routes = mock(StorageRoutes.class);
        S3ObjectStorageClient s3Client = mock(S3ObjectStorageClient.class);
        MediaReadStorageServiceImpl primaryStorage =
                new MediaReadStorageServiceImpl(PRIMARY_STORAGE_NAME, s3Client);

        when(routes.getPrimaryStorage()).thenReturn(primaryStorage);
        when(routes.getServiceBucketMap())
                .thenReturn(Map.of(PRIMARY_STORAGE_NAME, PRIMARY_BUCKET_NAME));

        when(s3Client.listBuckets()).thenReturn(List.of(
                Bucket.builder().name("another-bucket").build()
        ));

        PrimaryStorageHealthIndicator indicator = new PrimaryStorageHealthIndicator(routes);
        Health health = indicator.health();
        assertEquals("DOWN", health.getStatus().getCode());
    }


    @Test
    void primaryStorageIsDownWhenBucketCheckThrowsException() {
        StorageRoutes routes = mock(StorageRoutes.class);
        S3ObjectStorageClient s3Client = mock(S3ObjectStorageClient.class);
        MediaReadStorageServiceImpl primaryStorage =
                new MediaReadStorageServiceImpl(PRIMARY_STORAGE_NAME, s3Client);

        when(routes.getPrimaryStorage()).thenReturn(primaryStorage);
        when(routes.getServiceBucketMap())
                .thenReturn(Map.of(PRIMARY_STORAGE_NAME, PRIMARY_BUCKET_NAME));
        when(s3Client.listBuckets())
                .thenThrow(new RuntimeException("S3 connection failed"));

        PrimaryStorageHealthIndicator indicator = new PrimaryStorageHealthIndicator(routes);

        Health health = indicator.health();
        assertEquals("DOWN", health.getStatus().getCode());
    }



    @Test
    void primaryStorageIsDownWhenNoBucketIsConfigured() {
        StorageRoutes routes = mock(StorageRoutes.class);
        MediaReadStorageServiceImpl primaryStorage =
                new MediaReadStorageServiceImpl(PRIMARY_STORAGE_NAME, mock(S3ObjectStorageClient.class));

        when(routes.getPrimaryStorage()).thenReturn(primaryStorage);
        when(routes.getServiceBucketMap()).thenReturn(Map.of());

        PrimaryStorageHealthIndicator indicator = new PrimaryStorageHealthIndicator(routes);
        Health health = indicator.health();
        assertEquals("DOWN", health.getStatus().getCode());
    }


    @Test
    void primaryStorageIsDownWhenStorageIsNotS3() {
        StorageRoutes routes = mock(StorageRoutes.class);
        MediaReadStorageService primaryStorage = mock(MediaReadStorageService.class);

        when(primaryStorage.getName()).thenReturn("non-s3-storage");
        when(routes.getPrimaryStorage()).thenReturn(primaryStorage);
        when(routes.getServiceBucketMap()).thenReturn(Map.of("non-s3-storage", PRIMARY_BUCKET_NAME));

        PrimaryStorageHealthIndicator indicator = new PrimaryStorageHealthIndicator(routes);
        Health health = indicator.health();
        assertEquals("DOWN", health.getStatus().getCode());
    }


    @Test
    void primaryStorageIsDownWhenPrimaryStorageIsNotConfigured() {
        StorageRoutes routes = mock(StorageRoutes.class);
        when(routes.getPrimaryStorage())
                .thenThrow(new IllegalStateException("No storage configured"));

        PrimaryStorageHealthIndicator indicator =  new PrimaryStorageHealthIndicator(routes);
        Health health = indicator.health();
        assertEquals("DOWN", health.getStatus().getCode());
    }



}
