package eu.europeana.thumbnail.service;

import eu.europeana.domain.ObjectMetadata;
import eu.europeana.thumbnail.model.MediaFile;

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
     * @return an object which contains all the metadata and image content, or null if  there was an error
     */
    MediaFile retrieveAsMediaFile(String id, String originalUrl);

    /**
     * Retrieves only the content of a media file (so no metadata)
     *
     * @param id the id of the file
     * @return an array of bytes representing only the media content of the file
     */
    byte[] retrieveContent(String id);

    /**
     * Retrieves only the metadata of a media file
     *
     * @param id the id of the file
     * @return ObjectMetadata object
     */
    ObjectMetadata retrieveMetaData(String id);

    /**
     * Return the name of this storage, as used in the configuration
     *
     * @return name of this storage
     */
    String getName();

}
