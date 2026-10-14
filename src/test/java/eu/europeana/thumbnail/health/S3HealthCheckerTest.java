
package eu.europeana.thumbnail.health;

import eu.europeana.s3.S3ObjectStorageClient;
import eu.europeana.thumbnail.config.StorageRoutes;
import eu.europeana.thumbnail.service.MediaReadStorageService;
import eu.europeana.thumbnail.service.impl.MediaReadStorageServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import software.amazon.awssdk.services.s3.model.Bucket;

class S3HealthCheckerTest {

    @Test
    void primaryStorageIsSkipped() {
        StorageRoutes routes = mock(StorageRoutes.class);
        S3ObjectStorageClient primaryClient = mock(S3ObjectStorageClient.class);
        S3ObjectStorageClient secondaryClient = mock(S3ObjectStorageClient.class);
        MediaReadStorageServiceImpl primaryStorage = new MediaReadStorageServiceImpl("metis-prod", primaryClient);
        MediaReadStorageServiceImpl secondaryStorage = new MediaReadStorageServiceImpl("metis-test", secondaryClient);

        when(routes.getPrimaryStorage()).thenReturn(primaryStorage);
        when(routes.getMediaStorageServices()).thenReturn(Map.of(
                "metis-prod", primaryStorage,
                "metis-test", secondaryStorage
        ));
        when(routes.getServiceBucketMap()).thenReturn(Map.of(
                "metis-prod", "primary-bucket",
                "metis-test", "secondary-bucket"
        ));

        when(secondaryClient.listBuckets()).thenReturn(List.of( Bucket.builder().name("secondary-bucket").build()));

        S3HealthChecker checker = new S3HealthChecker(routes);
        Health health = checker.health();

        assertEquals("UP", health.getStatus().getCode());
        verify(primaryClient, org.mockito.Mockito.never()).listBuckets();
        verify(secondaryClient).listBuckets();
    }


    @Test
    void secondaryStorageIsDownWhenBucketIsMissing() {
        StorageRoutes routes = mock(StorageRoutes.class);
        S3ObjectStorageClient primaryClient = mock(S3ObjectStorageClient.class);
        S3ObjectStorageClient secondaryClient = mock(S3ObjectStorageClient.class);
        MediaReadStorageServiceImpl primaryStorage =
                new MediaReadStorageServiceImpl("metis-prod", primaryClient);
        MediaReadStorageServiceImpl secondaryStorage =
                new MediaReadStorageServiceImpl("metis-test", secondaryClient);

        when(routes.getPrimaryStorage()).thenReturn(primaryStorage);
        when(routes.getMediaStorageServices()).thenReturn(Map.of(
                "metis-prod", primaryStorage,
                "metis-test", secondaryStorage
        ));
        when(routes.getServiceBucketMap()).thenReturn(Map.of(
                "metis-prod", "primary-bucket",
                "metis-test", "secondary-bucket"
        ));

        when(secondaryClient.listBuckets()).thenReturn(List.of(
                Bucket.builder().name("another-bucket").build()
        ));

        S3HealthChecker checker = new S3HealthChecker(routes);
        Health health = checker.health();
        assertEquals("DOWN", health.getStatus().getCode());
        verify(primaryClient, org.mockito.Mockito.never()).listBuckets();
        verify(secondaryClient).listBuckets();
    }


    @Test
    void secondaryStorageIsDownWhenBucketCheckThrowsException() {
        StorageRoutes routes = mock(StorageRoutes.class);
        S3ObjectStorageClient primaryClient = mock(S3ObjectStorageClient.class);
        S3ObjectStorageClient secondaryClient = mock(S3ObjectStorageClient.class);
        MediaReadStorageServiceImpl primaryStorage =
                new MediaReadStorageServiceImpl("metis-prod", primaryClient);
        MediaReadStorageServiceImpl secondaryStorage =
                new MediaReadStorageServiceImpl("metis-test", secondaryClient);

        when(routes.getPrimaryStorage()).thenReturn(primaryStorage);
        when(routes.getMediaStorageServices()).thenReturn(Map.of(
                "metis-prod", primaryStorage,
                "metis-test", secondaryStorage
        ));
        when(routes.getServiceBucketMap()).thenReturn(Map.of(
                "metis-prod", "primary-bucket",
                "metis-test", "secondary-bucket"
        ));

        when(secondaryClient.listBuckets()).thenThrow(new RuntimeException("S3 connection failed"));

        S3HealthChecker checker = new S3HealthChecker(routes);
        Health health = checker.health();
        assertEquals("DOWN", health.getStatus().getCode());
        verify(primaryClient, org.mockito.Mockito.never()).listBuckets();
        verify(secondaryClient).listBuckets();
    }


    @Test
    void secondaryStorageIsDownWhenStorageIsNotS3() {
        StorageRoutes routes = mock(StorageRoutes.class);
        S3ObjectStorageClient primaryClient = mock(S3ObjectStorageClient.class);
        MediaReadStorageServiceImpl primaryStorage =
                new MediaReadStorageServiceImpl("metis-prod", primaryClient);
        MediaReadStorageService secondaryStorage = mock(MediaReadStorageService.class);

        when(secondaryStorage.getName()).thenReturn("non-s3-storage");
        when(routes.getPrimaryStorage()).thenReturn(primaryStorage);
        when(routes.getMediaStorageServices()).thenReturn(Map.of(
                "metis-prod", primaryStorage,
                "non-s3-storage", secondaryStorage
        ));
        when(routes.getServiceBucketMap()).thenReturn(Map.of(
                "metis-prod", "primary-bucket",
                "non-s3-storage", "secondary-bucket"
        ));

        S3HealthChecker checker = new S3HealthChecker(routes);
        Health health = checker.health();
        assertEquals("DOWN", health.getStatus().getCode());
        verify(primaryClient, org.mockito.Mockito.never()).listBuckets();
    }



}
