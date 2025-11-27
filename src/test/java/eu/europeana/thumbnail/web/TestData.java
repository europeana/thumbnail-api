package eu.europeana.thumbnail.web;

import eu.europeana.features.S3Object;
import eu.europeana.thumbnail.model.ImageSize;
import eu.europeana.thumbnail.model.MediaStream;
import eu.europeana.thumbnail.service.MediaReadStorageService;
import eu.europeana.thumbnail.service.StoragesService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

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

    public static final Instant LAST_MODIFIED_DATE = Instant.ofEpochMilli(1600000000000L);
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

    public static final boolean INITIALIZED = false;

    public static void defaultSetup(StoragesService storagesService, MediaReadStorageService mediaStorage) {
        // HACK: this method is called often, but here we make sure we only run it once
        if (!INITIALIZED) {
            Map<String, Object> metaDataLarge = new HashMap<>();
            metaDataLarge.put(S3Object.CONTENT_LENGTH, TestData.LARGE_CONTENT.getBytes().length);
            metaDataLarge.put(S3Object.LAST_MODIFIED, LAST_MODIFIED_DATE);
            metaDataLarge.put(S3Object.ETAG, TestData.ETAG);

            Map<String, Object> metaDataMedium = new HashMap<>();
            metaDataMedium.put(S3Object.CONTENT_LENGTH, TestData.MEDIUM_CONTENT.getBytes().length);
            metaDataMedium.put(S3Object.LAST_MODIFIED, LAST_MODIFIED_DATE);
            metaDataMedium.put(S3Object.ETAG, TestData.ETAG);

            // for v2 we send id and originalUrl
            given(mediaStorage.retrieve(TestData.URI_HASH + TestData.SIZE_LARGE, TestData.URI))
                    .willReturn(new MediaStream(TestData.URI_HASH + TestData.SIZE_LARGE, TestData.URI,
                            new S3Object(TestData.LARGE_STREAM, metaDataLarge)));
            given(mediaStorage.retrieve(TestData.URI_HASH + TestData.SIZE_MEDIUM, TestData.URI))
                    .willReturn(new MediaStream(TestData.URI_HASH + TestData.SIZE_MEDIUM, TestData.URI,
                            new S3Object(TestData.MEDIUM_STREAM, metaDataMedium)));

            // for v3 we send only id since originalUrl is not known
            given(mediaStorage.retrieve(TestData.URI_HASH + TestData.SIZE_LARGE, null))
                    .willReturn(new MediaStream(TestData.URI_HASH + TestData.SIZE_LARGE, null,
                            new S3Object(TestData.LARGE_STREAM, metaDataLarge)));
            given(mediaStorage.retrieve(TestData.URI_HASH + TestData.SIZE_MEDIUM, null))
                    .willReturn(new MediaStream(TestData.URI_HASH + TestData.SIZE_MEDIUM, null,
                            new S3Object(TestData.MEDIUM_STREAM, metaDataMedium)));

            List<MediaReadStorageService> storages = new ArrayList<>();
            storages.add(mediaStorage);
            given(storagesService.getStorages(anyString()))
                    .willReturn(storages);
        }
    }

}
