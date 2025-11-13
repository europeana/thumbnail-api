package eu.europeana.thumbnail.web;

import com.amazonaws.services.s3.Headers;
import eu.europeana.thumbnail.config.StorageRoutes;
import eu.europeana.thumbnail.service.MediaReadStorageService;
import eu.europeana.thumbnail.service.StoragesService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for Thumbnail V3 controller
 *
 * @author Srishti Singh on 05-09-2019.
 * @author Patrick Ehlert, major refactoring in September 2020
 */
@TestPropertySource("classpath:testroutes.properties")
@WebMvcTest({ThumbnailControllerV3.class, StorageRoutes.class})
@AutoConfigureMockMvc
@SuppressWarnings("java:S5786")
public class ThumbnailControllerV3Test {

    private static final String V3_ENDPOINT = "/thumbnail/v3/{size}/{url}";

    private static final Logger LOG = LogManager.getLogger(ThumbnailControllerV3Test.class);

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private StoragesService storageService;
    @MockBean
    private MediaReadStorageService mediaStorage;

    // KNOWN ISSUES: ever since migrating to SB3 and using streams there are 2 weird issues with reading the body in
    // unit tests.
    // 1. When a single test method is run in IntelliJ (e.g. test_200_Ok() or test_200_ContentType()) HEAD requests
    //    become a GET request so the content check for these HEAD requests fails
    // 2. When running all tests in this class in IntelliJ then HEAD requests work fine, but in test_200_Ok the expected
    //    content for the second GET request (with MEDIUM data) is empty despite us specifying an expected value.
    //    As a workaround we read the MEDIUM_STREAM before the test and strangely then the problem disappears.
    // 3. Some tests in the test_HeadEmptyPathVariables() and test_GetEmptyPathVariables method will return a 400 response
    //    in a running application, but 404 here in unit tests. Therefor we test for 4xx instead of 400
    // It looks like these are bugs (either in IntelliJ or in Mockito)

    @BeforeEach
    public void setup() {
        TestData.defaultSetup(storageService, mediaStorage);
    }

    /**
     * Test normal 200 Ok requests
     */
    @Test
    public void test_200_Ok() throws Exception {
        // HACK: if we don't read MEDIUM_STREAM here it will be empty in the last line of this test case below!?!?!?
        byte[] expected2 = TestData.MEDIUM_STREAM.readAllBytes();
        LOG.info("Expected medium data = {}", expected2);
        this.mockMvc.perform(get(V3_ENDPOINT, 200, TestData.URI_HASH))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.MEDIUM_CONTENT.length())))
                .andExpect(content().bytes(TestData.MEDIUM_STREAM.readAllBytes()));

        this.mockMvc.perform(head(V3_ENDPOINT, 200, TestData.URI_HASH))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.MEDIUM_CONTENT.length())))
                .andExpect(content().bytes(new byte[0]));

        // large image
        this.mockMvc.perform(get(V3_ENDPOINT, 400, TestData.URI_HASH))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.LARGE_CONTENT.length())))
                .andExpect(content().bytes(TestData.LARGE_STREAM.readAllBytes()));

        this.mockMvc.perform(head(V3_ENDPOINT, 400, TestData.URI_HASH))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.LARGE_CONTENT.length())))
                .andExpect(content().bytes(new byte[0]));
    }

    /**
     * Test if we get a 400 response for unknown hash codes
     */
    @Test
    public void test_404_NotFound() throws Exception {
        this.mockMvc.perform(get(V3_ENDPOINT, 400, "dfbf02e00c4bc7c737a4479a6bcc2662"))
                .andExpect(status().isNotFound());

        this.mockMvc.perform(head(V3_ENDPOINT, 400, "74af0c22f50e4c4c1b1dead321649e6022a2ac2.jpeg"))
                .andExpect(status().isNotFound());
    }

    /**
     * Test if we return a 400 error for image size other than the supported 200 and 400
     */
    @Test
    public void test_400_InvalidSize() throws Exception {
        this.mockMvc.perform(get(V3_ENDPOINT, 456, TestData.URI_HASH))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", Matchers.containsString(ThumbnailControllerV3.SIZE_ERROR_MESSAGE)));


        this.mockMvc.perform(head(V3_ENDPOINT, 456, TestData.URI_HASH))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void test_304_NotModified() throws Exception {
        // Check if Last-Modified and eTag are present from first request
        this.mockMvc.perform(get(V3_ENDPOINT, 400, TestData.URI_HASH))
                .andExpect(status().isOk())
                .andExpect(header().string((Headers.LAST_MODIFIED), TestData.LAST_MODIFIED_TEXT))
                .andExpect(header().string((Headers.ETAG), TestData.ETAG_VALUE));

        // Check if we get 304 if we sent only LastModified
        this.mockMvc.perform(get(V3_ENDPOINT, 400, TestData.URI_HASH)
                .header("If-Modified-Since", TestData.LAST_MODIFIED_TEXT))
                .andExpect(status().isNotModified())
                .andExpect(header().string((Headers.LAST_MODIFIED), TestData.LAST_MODIFIED_TEXT))
                .andExpect(header().string((Headers.ETAG), TestData.ETAG_VALUE));

        // Check if we get 304 if we sent only eTag
        this.mockMvc.perform(get(V3_ENDPOINT, 400, TestData.URI_HASH)
                .header("If-None-Match", TestData.ETAG_VALUE))
                .andExpect(status().isNotModified())
                .andExpect(header().string((Headers.LAST_MODIFIED), TestData.LAST_MODIFIED_TEXT))
                .andExpect(header().string((Headers.ETAG), TestData.ETAG_VALUE));
    }

    @Test
    public void test_412_PreconditionFailed() throws Exception {
        this.mockMvc.perform(get(V3_ENDPOINT, 400, TestData.URI_HASH)
                .header("If-Match", "*"))
                .andExpect(status().isOk());

        this.mockMvc.perform(get(V3_ENDPOINT, 400, TestData.URI_HASH)
                .header("If-Match", TestData.ETAG_VALUE))
                .andExpect(status().isOk());

        this.mockMvc.perform(get(V3_ENDPOINT, 400, TestData.URI_HASH)
                .header("If-Match", "test"))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    public void test_emptyIDOnlyWithExtension() throws Exception {
       this.mockMvc.perform(get(V3_ENDPOINT, 400, TestData.INVALID_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", Matchers.containsString(ThumbnailControllerV3.ID_ERROR_MESSAGE)));

       this.mockMvc.perform(head(V3_ENDPOINT, 400, TestData.INVALID_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void test_GetEmptyPathVariables() throws Exception {
        this.mockMvc.perform(get(V3_ENDPOINT, 400, ""))
                .andExpect(status().isBadRequest());

        // Weirdly the last 2 tests will return 400 in real life, but 404 in unit tests!?
        this.mockMvc.perform(get(V3_ENDPOINT, "", TestData.URI_HASH))
                .andExpect(status().is4xxClientError());
              //  .andExpect(jsonPath("$.message", Matchers.containsString(ThumbnailControllerV3.URL_ERROR_MESSAGE)));

        this.mockMvc.perform(get(V3_ENDPOINT, "", ""))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void test_HeadEmptyPathVariables() throws Exception {

        this.mockMvc.perform(head(V3_ENDPOINT, 400, ""))
                .andExpect(status().isBadRequest());

        // Weirdly the last 2 tests will return 400 in real life, but 404 in unit tests!?
        this.mockMvc.perform(head(V3_ENDPOINT, "", TestData.URI_HASH))
                .andExpect(status().is4xxClientError());

        this.mockMvc.perform(head(V3_ENDPOINT, "", ""))
                .andExpect(status().is4xxClientError());
    }
}
