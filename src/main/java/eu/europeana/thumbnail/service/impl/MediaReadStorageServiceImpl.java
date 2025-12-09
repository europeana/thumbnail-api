package eu.europeana.thumbnail.service.impl;

import eu.europeana.s3.S3Object;
import eu.europeana.s3.S3ObjectStorageClient;
import eu.europeana.thumbnail.model.MediaStream;
import eu.europeana.thumbnail.service.MediaReadStorageService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Service for retrieving media (e.g. thumbnails) from an object storage like Amazons S3 or IBM Cloud S3
 */
public class MediaReadStorageServiceImpl implements MediaReadStorageService {

    private static final Logger LOG = LogManager.getLogger(MediaReadStorageServiceImpl.class);

    private final String name;
    protected final S3ObjectStorageClient objectStorageClient;

    /**
     * Initialize a new MediaStorageService implementation
     * @param name the (informal) name of the storage
     * @param objectStorageClient the S3 client to use
     */
    public MediaReadStorageServiceImpl(String name, S3ObjectStorageClient objectStorageClient) {
        this.name = name;
        this.objectStorageClient = objectStorageClient;
    }

    /**
     * @see MediaReadStorageService#checkIfExists(String)
     */
    @Override
    public Boolean checkIfExists(String id) {
        return objectStorageClient.isObjectAvailable(id);
    }

    /**
     * @see MediaReadStorageService#retrieve(String, String)
     */
    @Override
    @SuppressWarnings("javasecurity:S5145") // we only log for debug purposes, plus we validate the user input
    public MediaStream retrieve(String id, String originalUrl) {
        LOG.debug("Retrieving file with id {}, url = {}", id, originalUrl);
        S3Object obj = objectStorageClient.getObject(id);
        if (obj == null || obj.inputStream() == null) {
            return null;
        } else {
            return new MediaStream(id, originalUrl, obj);
        }
    }

    /**
     * @see MediaReadStorageService#getName()
     */
    @Override
    public String getName() {
        return name;
    }
}



