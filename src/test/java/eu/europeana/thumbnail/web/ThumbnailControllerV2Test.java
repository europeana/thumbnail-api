package eu.europeana.thumbnail.web;

import eu.europeana.domain.Headers;
import eu.europeana.thumbnail.model.MediaFile;
import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.thumbnail.service.StoragesService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for Thumbnail V2 controller
 *
 * @author Srishti Singh on 14-08-2019.
 * @author Patrick Ehlert, major refactoring in September 2020
 */
@WebMvcTest({ThumbnailControllerV2.class})
public class ThumbnailControllerV2Test {

    public static final String V2_ENDPOINT        = "/api/v2/thumbnail-by-url.json";
    public static final String URI_PARAMETER      = "uri";

    private static final String URI_NO_TYPE        = "http://test.com/thumbnail";
    private static final String URI_NO_TYPE_HASH   = "700907be89102ec23950a909d51f2948";
    private static final String URI_PNG            = "http://test.com/thumbnail.png";
    private static final String URI_PNG_HASH       = "88532f3fe6c0992f7c10fff72d2f4735";
    private static final String URI_PDF            = "http://test.com/thumbnail.pdf";
    private static final String URI_PDF_HASH       = "050a24470934d17c7433cb4186a2843f";

    private static final String URI_NOT_PRESENT    = "http://test.com/image_not_found.jpg";
    private static final String URI_INVALID        = "test://test-thumbnail";

    private static final String DEFAULT_IMAGE_LENGTH = "2319";
    private static final String DEFAULT_VIDEO_LENGTH = "1932";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private StoragesService storagesService;
    @MockBean
    private MediaStorageService mediaStorage;

    @BeforeEach
    public void setup() {
        TestData.defaultSetup(storagesService, mediaStorage);
    }

    /**
     * Test normal 200 Ok requests
     */
    @Test
    public void test_200_Ok() throws Exception {
        // minimal request with only uri
        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.LARGE_FILE.length())))
                .andExpect(content().bytes(TestData.LARGE_CONTENT));

        // head request should return the same, but with empty content
        this.mockMvc.perform(head(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.LARGE_FILE.length())))
                .andExpect(content().string(""));

        // specify smaller size
        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI)
                .param("size", "w200"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.MEDIUM_FILE.length())))
                .andExpect(content().bytes(TestData.MEDIUM_CONTENT));

        // specify big size
        this.mockMvc.perform(head(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI)
                .param("size", "w400"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.LARGE_FILE.length())))
                .andExpect(content().string(""));
    }

    /**
     * Test if we get the expected content types
     */
    @Test
    public void test_200_ContentType() throws Exception {
        given(mediaStorage.retrieveAsMediaFile(URI_NO_TYPE_HASH + TestData.SIZE_LARGE, URI_NO_TYPE)).willReturn(
                new MediaFile(URI_NO_TYPE_HASH + TestData.SIZE_LARGE, URI_NO_TYPE, TestData.LARGE_CONTENT));
        given(mediaStorage.retrieveAsMediaFile(URI_PNG_HASH + TestData.SIZE_LARGE, URI_PNG)).willReturn(
                new MediaFile(URI_PNG_HASH + TestData.SIZE_LARGE, URI_PNG, TestData.LARGE_CONTENT));
        given(mediaStorage.retrieveAsMediaFile(URI_PDF_HASH + TestData.SIZE_LARGE, URI_PDF)).willReturn(
                new MediaFile(URI_PDF_HASH + TestData.SIZE_LARGE, URI_PDF, TestData.LARGE_CONTENT));

        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, URI_NO_TYPE))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(content().bytes(TestData.LARGE_CONTENT));

        this.mockMvc.perform(head(V2_ENDPOINT)
                .param(URI_PARAMETER, URI_PNG))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_PNG_VALUE)))
                .andExpect(content().string(""));

        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, URI_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_PNG_VALUE)))
                .andExpect(content().bytes(TestData.LARGE_CONTENT));
    }

    /**
     * Test Get and Head mapping Default response (when image was not found and we return the default icon)
     */
    @Test
    public void test_200_DefaultIcon() throws Exception {
        //for invalid Image and default icon
        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, URI_NOT_PRESENT))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_PNG_VALUE)))
                .andExpect(header().string("Content-Length", DEFAULT_IMAGE_LENGTH));

        //for invalid Video and default icon
        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, URI_NOT_PRESENT).param("type", "VIDEO"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_PNG_VALUE)))
                .andExpect(header().string("Content-Length", DEFAULT_VIDEO_LENGTH));

        this.mockMvc.perform(head(V2_ENDPOINT)
                .param(URI_PARAMETER, URI_NOT_PRESENT))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_PNG_VALUE)))
                .andExpect(header().string("Content-Length", DEFAULT_IMAGE_LENGTH));

        //for invalid Video and default icon
        this.mockMvc.perform(head(V2_ENDPOINT)
                .param(URI_PARAMETER, URI_NOT_PRESENT).param("type", "VIDEO"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_PNG_VALUE)))
                .andExpect(header().string("Content-Length", DEFAULT_VIDEO_LENGTH));
    }

    /**
     * Test Get and Head mapping Invalid URL schema Response
     */
    @Test
    public void test_400_InvalidURL() throws Exception {
        String error = this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, URI_INVALID))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""))
                .andReturn().getResolvedException().getMessage();

        assertTrue(StringUtils.contains(error, ThumbnailControllerV2.INVALID_URL_MESSAGE));

        error =  this.mockMvc.perform(head(V2_ENDPOINT)
                .param(URI_PARAMETER, URI_INVALID))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""))
                .andExpect(header().string("Content-Length", "0"))
                .andReturn().getResolvedException().getMessage();

        assertTrue(StringUtils.contains(error, ThumbnailControllerV2.INVALID_URL_MESSAGE));
    }

    @Test
    public void test_304_NotModified() throws Exception  {
        // Check if Last-Modified and eTag are present from first request
        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI))
                .andExpect(status().isOk())
                .andExpect(header().string((Headers.LAST_MODIFIED), TestData.LAST_MODIFIED_TEXT))
                .andExpect(header().string((Headers.ETAG), TestData.ETAG_VALUE));

        // Check if we get 304 if we sent only LastModified
        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI)
                .header("If-Modified-Since", TestData.LAST_MODIFIED_TEXT))
                .andExpect(status().isNotModified())
                .andExpect(header().string((Headers.LAST_MODIFIED), TestData.LAST_MODIFIED_TEXT))
                .andExpect(header().string((Headers.ETAG), TestData.ETAG_VALUE));

        // Check if we get 304 if we sent only eTag
        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI)
                .header("If-None-Match", TestData.ETAG_VALUE))
                .andExpect(status().isNotModified())
                .andExpect(header().string((Headers.LAST_MODIFIED), TestData.LAST_MODIFIED_TEXT))
                .andExpect(header().string((Headers.ETAG), TestData.ETAG_VALUE));
    }

    @Test
    public void test_412_PreconditionFailed() throws Exception {
        //for Valid ETag : *, 12345abcde
        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI)
                .header("If-Match", "*"))
                .andExpect(status().isOk());

        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI)
                .header("If-Match", TestData.ETAG_VALUE))
                .andExpect(status().isOk());

        //for invalid Etag : test
        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI)
                .header("If-Match", "test"))
                .andExpect(status().isPreconditionFailed());
    }

}
