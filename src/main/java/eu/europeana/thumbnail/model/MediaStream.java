package eu.europeana.thumbnail.model;

import eu.europeana.exception.S3ObjectStorageException;
import eu.europeana.features.S3Object;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;


/**
 * Wrapping class around an S3Object retrieved from object storage or a default icon or IIIF image loaded from a IIIF
 * server
 */
public class MediaStream {

    private final String id;
    private final String originalUrl;
    private final S3Object s3Object;

    private boolean closed = false;

    /**
     * Create a new media stream based on a retrieved or constructed S3 object.
     * @param id the id (hash) of the object
     * @param originalUrl optional, the original url of the object (only available for v2 requests)
     * @param s3Object the retrieved object from S3 storage
     */
    public MediaStream(String id, String originalUrl, S3Object s3Object) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.s3Object = s3Object;
    }

    /**
     * @return The md5 of the original url plus hyphen and size (This is how a thumbnail file is stored in S3)
     */
    public String getId() {
        return id;
    }

    /**
     * @return the original url of where the file was stored (for v2 only)
     */
    public String getOriginalUrl() {
        return originalUrl;
    }

    /**
     * @return the stored S3 object
     */
    public S3Object getS3Object() {
        return s3Object;
    }

    /**
     * @return true if the stored S3 object has metadata, otherwise false
     */
    public boolean hasMetadata() {
        return (s3Object != null && s3Object.metadata() != null);
    }

    /**
     * Returns the metadata of the S3 Object (inputstream)
     */
    @SuppressWarnings("java:S1168") // deliberately return null here if there is no metadata (as opposed to empty metadata map)
    public Map<String, Object> getMetadata() {
        if (hasMetadata()) {
            return s3Object.metadata();
        }
        return null;
    }

    /**
     * @return Content type set in the S3 object's metadata, null if not set.
     */
    public String getContentType() {
        if (s3Object == null) {
            return null;
        }
        return s3Object.getContentType();
    }

    /**
     * @return Length of the content in number of bytes (for both S3 objects and default icons), null if not available
     */
    public Long getContentLength() {
        if (s3Object == null) {
            return null;
        }
        return s3Object.getContentLength();
    }

    /**
     * @return date when the S3 object was last modified (if available)
     */
    public Instant getLastModified() {
        if (s3Object == null) {
            return null;
        }
        return s3Object.getLastModified();
    }

    /**
     * @return eTag of the S3 object (if available)
     */
    public String getETag() {
        if (s3Object == null) {
            return null;
        }
        return s3Object.getETag();
    }

    /**
     * Close the stream to the S3 Object. This must be done manually when the object is not sent out to a client
     * Failure to do so will result in connection leaks and eventually lack of connections in S3's connection pool.
     */
    public void close() {
        try {
            if (s3Object != null) {
                s3Object.inputStream().close();
            }
            this.closed = true;
        } catch (IOException e) {
            throw new S3ObjectStorageException("Error closing S3 object stream " + id, e);
        }
    }

    public boolean isClosed() {
        return closed;
    }

}


