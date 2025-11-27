package eu.europeana.thumbnail.model;

import eu.europeana.features.S3Object;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MediaStreamTest {

    @Test
    public void testNoMetadata() {
        MediaStream ms = new MediaStream("id", null, new S3Object(null, null));
        assertFalse(ms.hasMetadata());
        assertNull(ms.getMetadata());
    }

    @Test
    public void testMetadata() {
        String contentType = "text/html";
        Long contentLength = 10L;
        String eTag = "hi";
        Instant lastModified = Instant.now();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(S3Object.CONTENT_TYPE, contentType);
        metadata.put(S3Object.CONTENT_LENGTH, contentLength);
        metadata.put(S3Object.ETAG, eTag);
        metadata.put(S3Object.LAST_MODIFIED, lastModified);
        MediaStream ms = new MediaStream("id", null, new S3Object(null, metadata));

        assertTrue(ms.hasMetadata());
        assertNotNull(ms.getMetadata());
        assertEquals(contentType, ms.getContentType());
        assertEquals(contentLength, ms.getContentLength());
        assertEquals(eTag, ms.getETag());
        assertEquals(lastModified, ms.getLastModified());
    }

    @Test
    public void testStreamOpenClosed() {
        String testData = "This is a test data";
        MediaStream ms = new MediaStream("id", null, new S3Object(
            new ByteArrayInputStream(testData.getBytes()), null));
        assertFalse(ms.isClosed());
        ms.close();
        assertTrue(ms.isClosed());
    }
}
