package eu.europeana.thumbnail.model;

import com.amazonaws.services.s3.model.ObjectMetadata;
import org.joda.time.DateTime;

import java.io.InputStream;

/**
 * Wrapping class around an object retrieved from object storage.
 */
public class MediaStream {

    private final String id;
    private final String originalUrl;
    private final InputStream content;
    private final ObjectMetadata metaData;

    public MediaStream(String id, String originalUrl, InputStream content) {
        this(id, originalUrl, content, null);
    }

    public MediaStream(String id, String originalUrl, InputStream content, ObjectMetadata metadata) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.content = content;
        this.metaData = metadata;
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
    public InputStream getContent() {
        return content;
    }

    /**
     * @return true if the mediastream has metadata, otherwise false
     */
    public boolean hasMetadata() {
        return (metaData != null);
    }

    /**
     * @return Length of the content in number of bytes
     */
    public long getContentLength() {
        if (metaData == null) {
            return 0;
        }
        return metaData.getContentLength();
    }

    /**
     * @return date when the file was last modified (if available)
     */
    public DateTime getLastModified() {
        if (metaData == null || metaData.getLastModified() == null) {
            return null;
        }
        return new DateTime(metaData.getLastModified().getTime());
    }

    /**
     * @return eTag of the content (if available)
     */
    public String getETag() {
        if (metaData == null) {
            return null;
        }
        return metaData.getETag();
    }


}


