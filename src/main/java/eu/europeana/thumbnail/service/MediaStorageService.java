package eu.europeana.thumbnail.service;

import eu.europeana.thumbnail.model.MediaStream;

/**
 * Service for retrieving media (e.g. thumbnails) from an object storage like Amazons S3 or IBM Cloud S3
 */
public interface MediaStorageService {

    /**
     * Checks if a particular file with the provided id is present in the media storage
     *
     * @param id the id of the file
     * @return true if the file is present, false if it is not
     */
    Boolean checkIfExists(String id);

    /**
     * Retrieves a file from media storage given it's id
     *
     * @param id the id of the file
     * @param originalUrl the original url of the file, optional for S3 storage, required for IiifImageServer
     * @return an object which contains all the metadata and image content (as a stream), null if not found
     */
    MediaStream retrieve(String id, String originalUrl);

    /**
     * Return the name of this storage, as used in the configuration
     *
     * @return name of this storage
     */
    String getName();

}
