package eu.europeana.thumbnail.web;

import com.amazonaws.services.s3.model.ObjectMetadata;
import eu.europeana.thumbnail.model.ImageSize;
import eu.europeana.thumbnail.model.MediaStream;
import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.thumbnail.service.StoragesService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Test data for testing both the V2 and V3 controllers
 *
 * @author Patrick Ehlert
 * Created on 2 sep 2020
 */
public class TestData {

    public static final String URI                = "https://test.europeana.eu/thumbnail.jpg";
    public static final String URI_HASH           = "7463a193a468a1ff1a0c0f7d5933e54b";
    public static final String INVALID_ID           = ".jpg";

    public static final Date   LAST_MODIFIED_DATE = new Date(1600000000000L);
    public static final String LAST_MODIFIED_TEXT = "Sun, 13 Sep 2020 12:26:40 GMT";
    public static final String ETAG               = "1234test";
    public static final String ETAG_VALUE         = "\"" + ETAG + "\"";

    public static final String SIZE_LARGE         = "-" + ImageSize.LARGE.name();
    public static final String SIZE_MEDIUM        = "-" + ImageSize.MEDIUM.name();
    public static final String LARGE_CONTENT = "This is some dummy text data instead of an actual image file\n" +
            "And here is another line just to make the content a bit larger";
    public static final InputStream LARGE_STREAM = new ByteArrayInputStream(LARGE_CONTENT.getBytes());
    public static final String MEDIUM_CONTENT        = "medium-sized test data";
    public static final InputStream MEDIUM_STREAM = new ByteArrayInputStream(MEDIUM_CONTENT.getBytes());
    public static final String URI_HTTP           = "http://test.europeana.eu/thumbnail.jpg";
    public static final String URI_URN            = "urn:soundcloud:43668849";
    public static final String URI_FTP            = "ftp://test.europeana.eu/thumbnail.jpg";

    public static final boolean initialized = false;

    public static void defaultSetup(StoragesService storagesService, MediaStorageService mediaStorage) {
        // HACK: this method is called often, but here we make sure we only run it once
        if (!initialized) {
            ObjectMetadataMock metaDataLarge = new ObjectMetadataMock();
            metaDataLarge.setContentLength(TestData.LARGE_CONTENT.getBytes().length);
            metaDataLarge.setLastModified(LAST_MODIFIED_DATE);
            metaDataLarge.setETag(TestData.ETAG);

            ObjectMetadataMock metaDataMedium = new ObjectMetadataMock();
            metaDataMedium.setContentLength(TestData.MEDIUM_CONTENT.getBytes().length);
            metaDataMedium.setLastModified(LAST_MODIFIED_DATE);
            metaDataMedium.setETag(TestData.ETAG);

            // for v2 we send id and originalUrl
            given(mediaStorage.retrieve(TestData.URI_HASH + TestData.SIZE_LARGE, TestData.URI)).willReturn(
                    new MediaStream(TestData.URI_HASH + TestData.SIZE_LARGE, TestData.URI, TestData.LARGE_STREAM, metaDataLarge));
            given(mediaStorage.retrieve(TestData.URI_HASH + TestData.SIZE_MEDIUM, TestData.URI)).willReturn(
                    new MediaStream(TestData.URI_HASH + TestData.SIZE_MEDIUM, TestData.URI, TestData.MEDIUM_STREAM, metaDataMedium));

            // for v3 we send only id since originalUrl is not known
            given(mediaStorage.retrieve(TestData.URI_HASH + TestData.SIZE_LARGE, null)).willReturn(
                    new MediaStream(TestData.URI_HASH + TestData.SIZE_LARGE, null, TestData.LARGE_STREAM, metaDataLarge));
            given(mediaStorage.retrieve(TestData.URI_HASH + TestData.SIZE_MEDIUM, null)).willReturn(
                    new MediaStream(TestData.URI_HASH + TestData.SIZE_MEDIUM, null, TestData.MEDIUM_STREAM, metaDataMedium));

            List<MediaStorageService> storages = new ArrayList<>();
            storages.add(mediaStorage);
            given(storagesService.getStorages(anyString())).willReturn(storages);
        }
    }

    // Amazon SDK doesn't allow us to set an ETag, so we use a wrapping object
    private static final class ObjectMetadataMock extends ObjectMetadata {
        private String eTag;

        public void setETag(String eTag) {
            this.eTag = eTag;
        }

        public String getETag() {
            return this.eTag;
        }

    }
}
