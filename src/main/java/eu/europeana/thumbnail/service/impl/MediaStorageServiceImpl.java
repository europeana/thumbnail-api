package eu.europeana.thumbnail.service.impl;

import com.amazonaws.services.s3.model.S3Object;
import eu.europeana.features.S3ObjectStorageClient;
import eu.europeana.thumbnail.model.MediaStream;
import eu.europeana.thumbnail.service.MediaStorageService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @see eu.europeana.thumbnail.service.MediaStorageService
 */
public class MediaStorageServiceImpl implements MediaStorageService {

    private static final Logger LOG = LogManager.getLogger(MediaStorageServiceImpl.class);

    private String name;
    private S3ObjectStorageClient objectStorageClient;

    /**
     * Initialize a new MediaStorageService implementation
     * @param name the (informal) name of the storage
     * @param objectStorageClient the S3 client to use
     */
    public MediaStorageServiceImpl(String name, S3ObjectStorageClient objectStorageClient) {
        this.name = name;
        this.objectStorageClient = objectStorageClient;
    }

    /**
     * @see MediaStorageService#checkIfExists(String)
     */
    @Override
    public Boolean checkIfExists(String id) {
        return objectStorageClient.isObjectAvailable(id);
    }

    /**
     * @see MediaStorageService#retrieve(String, String)
     */
    @Override
    public MediaStream retrieve(String id, String originalUrl) {
        LOG.debug("Retrieving file with id {}, url = {}", id, originalUrl);
        S3Object obj = objectStorageClient.getObject(id);
        if (obj == null) {
            return null;
        } else {
            return new MediaStream(id, originalUrl, obj);
        }
    }

    /**
     * @see MediaStorageService#getName()
     */
    @Override
    public String getName() {
        return name;
    }
}



