package eu.europeana.thumbnail.web;

import eu.europeana.features.S3Object;
import eu.europeana.thumbnail.model.MediaStream;
import eu.europeana.thumbnail.service.MediaReadStorageService;
import eu.europeana.thumbnail.service.StoragesService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("classpath:testroutes.properties")
@SuppressWarnings("java:S5786")
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

    private static final Logger LOG = LogManager.getLogger(ThumbnailControllerV2Test.class);

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private StoragesService storagesService;
    @MockitoBean
    private MediaReadStorageService mediaStorage;

    @BeforeEach
    public void setup() {
        TestData.defaultSetup(storagesService, mediaStorage);
    }

    // KNOWN ISSUES: ever since migrating to SB3 and using streams there are 2 weird issues with reading the body in
    // unit tests.
    // 1. When a single test method is run in IntelliJ (e.g. test_200_Ok() or test_200_ContentType()) HEAD requests
    //    become a GET request so the content check for these HEAD requests fails
    // 2. When running all tests in this class in IntelliJ then HEAD requests work fine, but in test_200_Ok the expected
    //    content for the second GET request (with MEDIUM data) is empty despite us specifying an expected value.
    //    As a workaround we read the MEDIUM_STREAM before the test and strangely then the problem disappears.
    // It looks like these are bugs (either in IntelliJ or in Mockito)
    // Update Nov 2025: the above issues still persist, even with the most recent version of IntelliJ

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
                .andExpect(header().string("Content-Length", String.valueOf(TestData.LARGE_CONTENT.length())))
                .andExpect(content().bytes(TestData.LARGE_STREAM.readAllBytes()));

        // head request should return the same, but with empty content
        this.mockMvc.perform(head(V2_ENDPOINT)
                    .param(URI_PARAMETER, TestData.URI))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.LARGE_CONTENT.length())))
                .andExpect(content().bytes(new byte[0]));

        // HACK: if we don't read MEDIUM_STREAM here it will be empty in the last line of the test case below!?!?!?
        byte[] expected2 = TestData.MEDIUM_STREAM.readAllBytes();
        LOG.info("Expected medium data = {}", expected2);
        this.mockMvc.perform(get(V2_ENDPOINT)
                        .param(URI_PARAMETER, TestData.URI)
                        .param("size", "w200"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.MEDIUM_CONTENT.length())))
                .andExpect(content().bytes(TestData.MEDIUM_STREAM.readAllBytes()));

        // specify smaller size
        this.mockMvc.perform(get(V2_ENDPOINT)
                    .param(URI_PARAMETER, TestData.URI)
                    .param("size", "w200"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.MEDIUM_CONTENT.length())))
                .andExpect(content().bytes(TestData.MEDIUM_STREAM.readAllBytes()));

        // specify big size
        this.mockMvc.perform(head(V2_ENDPOINT)
                    .param(URI_PARAMETER, TestData.URI)
                    .param("size", "w400"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.LARGE_CONTENT.length())))
                .andExpect(content().bytes(new byte[0]));
    }

    /**
     * Test if we get the expected content types
     */
    @Test
    public void test_200_ContentType() throws Exception {
        given(mediaStorage.retrieve(URI_NO_TYPE_HASH + TestData.SIZE_LARGE, URI_NO_TYPE)).willReturn(
                new MediaStream(URI_NO_TYPE_HASH + TestData.SIZE_LARGE, URI_NO_TYPE,
                        new S3Object(TestData.LARGE_STREAM, null)));
        given(mediaStorage.retrieve(URI_PNG_HASH + TestData.SIZE_LARGE, URI_PNG)).willReturn(
                new MediaStream(URI_PNG_HASH + TestData.SIZE_LARGE, URI_PNG,
                        new S3Object(TestData.LARGE_STREAM, null)));
        given(mediaStorage.retrieve(URI_PDF_HASH + TestData.SIZE_LARGE, URI_PDF)).willReturn(
                new MediaStream(URI_PDF_HASH + TestData.SIZE_LARGE, URI_PDF,
                        new S3Object(TestData.LARGE_STREAM, null)));

        this.mockMvc.perform(get(V2_ENDPOINT)
                    .param(URI_PARAMETER, URI_NO_TYPE))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(content().bytes(TestData.LARGE_STREAM.readAllBytes()));

        this.mockMvc.perform(head(V2_ENDPOINT)
                    .param(URI_PARAMETER, URI_PNG))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_PNG_VALUE)))
                .andExpect(content().bytes(new byte[0]));

        this.mockMvc.perform(get(V2_ENDPOINT)
                    .param(URI_PARAMETER, URI_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_PNG_VALUE)))
                .andExpect(content().bytes(TestData.LARGE_STREAM.readAllBytes()));
    }

    /**
     * Test Get and Head mapping Default response (when image was not found and we return the default icon)
     */
    @Test
    public void test_200_DefaultIcon() throws Exception {
        // for invalid Image and default icon
        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, URI_NOT_PRESENT))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_PNG_VALUE)))
                .andExpect(header().string("Content-Length", DEFAULT_IMAGE_LENGTH));

        // for invalid Video and default icon
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
       this.mockMvc.perform(get(V2_ENDPOINT)
                    .param(URI_PARAMETER, URI_INVALID))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message", Matchers.containsString(ThumbnailControllerV2.INVALID_URL_MESSAGE)));

       this.mockMvc.perform(head(V2_ENDPOINT)
                       .param(URI_PARAMETER, URI_INVALID))
               .andExpect(status().isBadRequest());
    }

    @Test
    public void test_304_NotModified() throws Exception  {
        // Check if Last-Modified and eTag are present from first request
        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI))
                .andExpect(status().isOk())
                .andExpect(header().string((S3Object.LAST_MODIFIED), TestData.LAST_MODIFIED_TEXT))
                .andExpect(header().string((S3Object.ETAG), TestData.ETAG_VALUE));

        // Check if we get 304 if we sent only LastModified
        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI)
                .header("If-Modified-Since", TestData.LAST_MODIFIED_TEXT))
                .andExpect(status().isNotModified())
                .andExpect(header().string((S3Object.LAST_MODIFIED), TestData.LAST_MODIFIED_TEXT))
                .andExpect(header().string((S3Object.ETAG), TestData.ETAG_VALUE));

        // Check if we get 304 if we sent only eTag
        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI)
                .header("If-None-Match", TestData.ETAG_VALUE))
                .andExpect(status().isNotModified())
                .andExpect(header().string((S3Object.LAST_MODIFIED), TestData.LAST_MODIFIED_TEXT))
                .andExpect(header().string((S3Object.ETAG), TestData.ETAG_VALUE));
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

    @Test
    public void test_ValidURls() throws Exception {

        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI))
                .andExpect(status().isOk());

        this.mockMvc.perform(get(V2_ENDPOINT)
                .param(URI_PARAMETER, TestData.URI_HTTP))
                .andExpect(status().isOk());

        this.mockMvc.perform(get(V2_ENDPOINT)
                    .param(URI_PARAMETER, TestData.URI_URN))
                .andExpect(status().isOk());

        this.mockMvc.perform(get(V2_ENDPOINT)
                    .param(URI_PARAMETER, TestData.URI_FTP))
                .andExpect(status().isOk());

        this.mockMvc.perform(get(V2_ENDPOINT)
                    .param(URI_PARAMETER, TestData.MEDIUM_CONTENT))
                .andExpect(status().isBadRequest());
    }
}
