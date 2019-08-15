package eu.europeana.thumbnail.config;

import eu.europeana.features.ObjectStorageClient;
import eu.europeana.features.S3ObjectStorageClient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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

    private static final Logger LOG = LogManager.getLogger(ThumbnailConfiguration.class);

    @Value("${thumbnail-api.baseurl}")
    private String thumbnailApiBaseUrl;
    @Value("${thumbnail-api.path}")
    private String thumbnailApiPath;

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

    /**
     * @return base url from where we should retrieve thumbnail json data
     */
    public String getThumbnailApiBaseUrl() {
        return thumbnailApiBaseUrl;
    }

    /**
     * @return Record resource path (should be appended to the thumbnail API base url)
     */
    public String getThumbnailApiPath() {
        return thumbnailApiPath;
    }

    /**
     * @return Metis Thumbnails hosted on IBM Cloud S3
     */

    public String getMetisThumbnailS3Key() {
        return metisThumbnailS3Key;
    }

    public String getMetisThumbnailS3Secret() {
        return metisThumbnailS3Secret;
    }

    public String getMetisThumbnailS3Region() {
        return metisThumbnailS3Region;
    }

    public String getMetisThumbnailS3Bucket() {
        return metisThumbnailS3Bucket;
    }

    public String getMetisThumbnailS3Endpoint() {
        return metisThumbnailS3Endpoint;
    }


    /**
     * @return UIM Thumbnails hosted on IBM Cloud S3
     */

    public String getUimThumbnailS3Key() {
        return uimThumbnailS3Key;
    }

    public String getUimThumbnailS3Secret() {
        return uimThumbnailS3Secret;
    }

    public String getUimThumbnailS3Region() {
        return uimThumbnailS3Region;
    }

    public String getUimThumbnailS3Bucket() {
        return uimThumbnailS3Bucket;
    }

    @Bean
    public ObjectStorageClient metisobjectStorageClient() {
        return new S3ObjectStorageClient(metisThumbnailS3Key, metisThumbnailS3Secret, metisThumbnailS3Region, metisThumbnailS3Bucket, metisThumbnailS3Endpoint);

    }
    @Bean
    public ObjectStorageClient uimObjectStorageClient() {

      return new S3ObjectStorageClient(uimThumbnailS3Key, uimThumbnailS3Secret, uimThumbnailS3Region, uimThumbnailS3Bucket);
    }

    /**
     * Note: this does not work when running the exploded build from the IDE because the values in the build.properties
     * are substituted only in the .war file. It returns 'default' in that case.
     * @return String containing app version, used in the eTag SHA hash generation
     */
    public String getAppVersion() {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/build.properties");
        if (resourceAsStream == null) {
            return "no version set";
        }
        try {
            Properties buildProperties = new Properties();
            buildProperties.load(resourceAsStream);
            return buildProperties.getProperty("info.app.version");
        } catch (IOException | RuntimeException e) {
            LOG.warn("Error reading version from build.properties file", e);
            return "no version set";
        }
    }

    @PostConstruct
    private void logImportantSettings() {
        LOG.info("Thumbnail settings:");
        LOG.info("  Thumbnail API Url = {}{} ", this.getThumbnailApiBaseUrl(), this.getThumbnailApiPath());
    }
}
