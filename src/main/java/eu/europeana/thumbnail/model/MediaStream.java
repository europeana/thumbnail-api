package eu.europeana.thumbnail.model;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapping class around a stream or S3Object retrieved from object storage.
 */
public class MediaStream {

    private final String id;
    private final String originalUrl;
    private final InputStream inputStream;
    private final ObjectMetadata metadata;
    private final S3Object s3object;

    private boolean closed = false;

    /**
     * Create a new media stream based on an existing inputstream
     * @param id the id (hash) of the object
     * @param originalUrl optional, the original url of the object (only available for v2 requests)
     * @param inputStream the input stream to use
     */
    public MediaStream(String id, String originalUrl, InputStream inputStream) {
        this(id, originalUrl, inputStream, null);
    }

    /**
     * Create a new media stream based on an existing inputstream
     * @param id the id (hash) of the object
     * @param originalUrl optional, the original url of the object (only available for v2 requests)
     * @param inputStream the input stream to use
     * @param metadata the metadata of the provided input stream
     */
    public MediaStream(String id, String originalUrl, InputStream inputStream, ObjectMetadata metadata) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.inputStream = inputStream;
        this.metadata = metadata;
        this.s3object = null;
    }

    /**
     * Create a new media stream based on object read from S3 storage
     * @param id the id (hash) of the object
     * @param originalUrl optional, the original url of the object (only available for v2 requests)
     * @param s3object the s3object (with metadata) to use
     */
    public MediaStream(String id, String originalUrl, S3Object s3object) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.inputStream = null;
        this.metadata = s3object.getObjectMetadata();
        this.s3object = s3object;
    }

    /**
     * @return The md5 of the original url plus hyphen and size (This is how a thumbnail file is stored in S3)
     */
    public String getId() {
        return id;
    }

    /**
     * @return the original url of where the file was stored
     */
    public String getOriginalUrl() {
        return originalUrl;
    }

    /**
     * @return actual content as stream
     */
    public InputStream getInputStream() {
        if (inputStream != null) {
            return inputStream;
        }
        return s3object.getObjectContent().getDelegateStream();
    }

    /**
     * @return true if the mediastream has metadata, otherwise false
     */
    public boolean hasMetadata() {
        return (metadata != null);
    }

    /**
     * @return Length of the content in number of bytes
     */
    public long getContentLength() {
        if (hasMetadata()) {
            return metadata.getContentLength();
        }
        return 0;
    }

    /**
     * @return date when the file was last modified (if available)
     */
    public DateTime getLastModified() {
        if (hasMetadata() && metadata.getLastModified() != null) {
            return new DateTime(metadata.getLastModified().getTime());
        }
        return null;
    }

    /**
     * @return eTag of the content (if available)
     */
    public String getETag() {
        if (hasMetadata()) {
            return metadata.getETag();
        }
        return null;
    }

    /**
     * Close the stream to the S3 Object. This must be done manually when the object is not sent out to a client
     * Failure to do so will result in connection leaks and eventually lack of connections in S3's connection pool.
     */
    public void close() {
        try {
            if (inputStream != null) {
                inputStream.close();
            } else {
                s3object.close();
            }
            this.closed = true;
        } catch (IOException e) {
            throw new RuntimeException("Error closing s3Object " + id, e);
        }
    }

    public boolean isClosed() {
        return closed;
    }


}


