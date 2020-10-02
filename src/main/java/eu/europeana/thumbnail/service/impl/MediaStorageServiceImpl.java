package eu.europeana.thumbnail.service.impl;

import eu.europeana.domain.ObjectMetadata;
import eu.europeana.domain.StorageObject;
import eu.europeana.features.ObjectStorageClient;
import eu.europeana.thumbnail.model.MediaFile;
import eu.europeana.thumbnail.service.MediaStorageService;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jclouds.io.Payload;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * @see eu.europeana.thumbnail.service.MediaStorageService
 */
public class MediaStorageServiceImpl implements MediaStorageService {

    private static final Logger LOG = LogManager.getLogger(MediaStorageServiceImpl.class);

    private String name;
    private ObjectStorageClient objectStorageClient;

    public MediaStorageServiceImpl(String name, ObjectStorageClient objectStorageClient) {
        this.name = name;
        this.objectStorageClient = objectStorageClient;
    }

    /**
     * @see MediaStorageService#checkIfExists(String)
     */
    @Override
    public Boolean checkIfExists(String id) {
        return objectStorageClient.isAvailable(id);
    }

    /**
     * @see MediaStorageService#retrieveAsMediaFile(String, String)
     */
    @Override
    public MediaFile retrieveAsMediaFile(String id, String originalUrl) {
        LOG.debug("Retrieving file with id {}, url = {}", id, originalUrl);
        StorageObject storageObject = retrieveAsStorageObject(id, true);
        if (storageObject == null) {
            return null;
        }

        byte[] content = null;
        try {
            content = convertPayloadToByteArray(storageObject.getPayload());
        } catch (IOException e) {
            LOG.error("Error reading media file contents, id = {}, url = {}", id, originalUrl, e);
        }
        if (content != null && content.length > 0) {
            return new MediaFile(storageObject.getName(), originalUrl, content, storageObject.getMetadata());
        }
        return null;
    }

    private StorageObject retrieveAsStorageObject(String id, boolean withContent) {
        final Optional<StorageObject> optStorageObject = withContent ? objectStorageClient.get(id) : objectStorageClient
                .getWithoutBody(id);

        return optStorageObject.orElse(null);
    }

    private byte[] convertPayloadToByteArray(Payload payload) throws IOException {
        byte[] result;
        try (InputStream in = payload.openStream()) {
            result = IOUtils.toByteArray(in);
        }
        return result;
    }

    /**
     * @see MediaStorageService#retrieveContent(String)
     */
    @Override
    public byte[] retrieveContent(String id) {return objectStorageClient.getContent(id); }

    /**
     * @see MediaStorageService#retrieveMetaData(String)
     */
    @Override
    public ObjectMetadata retrieveMetaData(String id) {
        return objectStorageClient.getMetaData(id);
    }

    /**
     * @see MediaStorageService#getName()
     */
    @Override
    public String getName() {
        return name;
    }
}



