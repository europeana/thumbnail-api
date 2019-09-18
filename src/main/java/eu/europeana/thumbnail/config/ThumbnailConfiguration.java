package eu.europeana.thumbnail.config;


import eu.europeana.features.S3ObjectStorageClient;
import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.thumbnail.service.impl.MediaStorageServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Container for all manifest settings that we load from the iiif.properties file. Note that we also have hard-coded
 * properties in the Definitions class
 * @author Srishti Singh
 * Created on 12-08-2019
 */
@Configuration
@Component
@PropertySource("classpath:thumbnail.properties")
@PropertySource(value = "classpath:thumbnail.user.properties", ignoreResourceNotFound = true)
public class ThumbnailConfiguration {

    @Value("${metis.thumbnails.s3.key}")
    private String metisThumbnailS3Key;
    @Value("${metis.thumbnails.s3.secret}")
    private String metisThumbnailS3Secret;
    @Value("${metis.thumbnails.s3.region}")
    private String metisThumbnailS3Region;
    @Value("${metis.thumbnails.s3.bucket}")
    private String metisThumbnailS3Bucket;
    @Value("${metis.thumbnails.s3.endpoint}")
    private String metisThumbnailS3Endpoint;

    @Value("${uim.thumbnails.s3.key}")
    private String uimThumbnailS3Key;
    @Value("${uim.thumbnails.s3.secret}")
    private String uimThumbnailS3Secret;
    @Value("${uim.thumbnails.s3.region}")
    private String uimThumbnailS3Region;
    @Value("${uim.thumbnails.s3.bucket}")
    private String uimThumbnailS3Bucket;

    @Bean
    public MediaStorageService metisobjectStorageClient() {
        return new MediaStorageServiceImpl(new S3ObjectStorageClient(metisThumbnailS3Key, metisThumbnailS3Secret, metisThumbnailS3Region, metisThumbnailS3Bucket, metisThumbnailS3Endpoint));

    }
    @Bean
    public MediaStorageService uimObjectStorageClient() {

      return new MediaStorageServiceImpl(new S3ObjectStorageClient(uimThumbnailS3Key, uimThumbnailS3Secret, uimThumbnailS3Region, uimThumbnailS3Bucket));
    }

}
