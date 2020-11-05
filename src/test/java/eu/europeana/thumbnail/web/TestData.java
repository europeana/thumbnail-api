package eu.europeana.thumbnail.web;

import eu.europeana.domain.ObjectMetadata;
import eu.europeana.thumbnail.model.ImageSize;
import eu.europeana.thumbnail.model.MediaFile;
import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.thumbnail.service.StoragesService;

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
    public static final String LARGE_FILE         = "This is some dummy text data instead of an actual image file";
    public static final byte[] LARGE_CONTENT      = LARGE_FILE.getBytes();
    public static final String MEDIUM_FILE        = "test data";
    public static final byte[] MEDIUM_CONTENT     = MEDIUM_FILE.getBytes();


    public static void defaultSetup(StoragesService storagesService, MediaStorageService mediaStorage) {
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setLastModified(TestData.LAST_MODIFIED_DATE);
        metaData.setETag(TestData.ETAG);

        // for v2 we send id and originalUrl
        given(mediaStorage.retrieveAsMediaFile(TestData.URI_HASH + TestData.SIZE_LARGE, TestData.URI)).willReturn(
                new MediaFile(TestData.URI_HASH + TestData.SIZE_LARGE, TestData.URI, TestData.LARGE_CONTENT, metaData));
        given(mediaStorage.retrieveAsMediaFile(TestData.URI_HASH + TestData.SIZE_MEDIUM, TestData.URI)).willReturn(
                new MediaFile(TestData.URI_HASH + TestData.SIZE_MEDIUM, TestData.URI, TestData.MEDIUM_CONTENT, metaData));

        // for v3 we send only id since originalUrl is not known
        given(mediaStorage.retrieveAsMediaFile(TestData.URI_HASH + TestData.SIZE_LARGE, null)).willReturn(
                new MediaFile(TestData.URI_HASH + TestData.SIZE_LARGE, null, TestData.LARGE_CONTENT, metaData));
        given(mediaStorage.retrieveAsMediaFile(TestData.URI_HASH + TestData.SIZE_MEDIUM, null)).willReturn(
                new MediaFile(TestData.URI_HASH + TestData.SIZE_MEDIUM, null, TestData.MEDIUM_CONTENT, metaData));

        List<MediaStorageService> storages = new ArrayList<>();
        storages.add(mediaStorage);
        given(storagesService.getStorages(anyString())).willReturn(storages);
    }
}
