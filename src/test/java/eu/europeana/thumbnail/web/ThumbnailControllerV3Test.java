package eu.europeana.thumbnail.web;

import eu.europeana.domain.Headers;
import eu.europeana.thumbnail.config.StorageRoutes;
import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.thumbnail.service.StoragesService;
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
public class ThumbnailControllerV3Test {

    private static final String V3_ENDPOINT = "/thumbnail/v3/{size}/{url}";
    private static final String UTF8_CHARSET = ";charset=UTF-8";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private StoragesService storageService;
    @MockBean
    private MediaStorageService mediaStorage;


    @BeforeEach
    public void setup() {
        TestData.defaultSetup(storageService, mediaStorage);
    }

    /**
     * Test normal 200 Ok requests
     */
    @Test
    public void test_200_Ok() throws Exception {
        // small image
        this.mockMvc.perform(get(V3_ENDPOINT, 200, TestData.URI_HASH))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.MEDIUM_FILE.length())))
                .andExpect(content().bytes(TestData.MEDIUM_CONTENT));

        this.mockMvc.perform(head(V3_ENDPOINT, 200, TestData.URI_HASH))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.MEDIUM_FILE.length())));
        // since migrating to SB3 Mockito returns content for head requests, despite SB3 still handling them properly
        //.andExpect(content().string(""));

        // large image
        this.mockMvc.perform(get(V3_ENDPOINT, 400, TestData.URI_HASH))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.LARGE_FILE.length())))
                .andExpect(content().bytes(TestData.LARGE_CONTENT));

        this.mockMvc.perform(head(V3_ENDPOINT, 400, TestData.URI_HASH))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", String.valueOf(TestData.LARGE_FILE.length())));
        // since migrating to SB3 Mockito returns content for head requests, despite SB3 still handling them properly
        //.andExpect(content().string(""));
    }

    /**
     * Test if we get a 400 response for unknown hash codes
     */
    @Test
    public void test_404_NotFound() throws Exception {
        this.mockMvc.perform(get(V3_ENDPOINT, 400, "hashNotFound"))
                .andExpect(status().isNotFound());

        this.mockMvc.perform(head(V3_ENDPOINT, 400, "hashNotFound"))
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
                .andExpect(status().is4xxClientError());


        this.mockMvc.perform(get(V3_ENDPOINT, "", TestData.URI_HASH))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", Matchers.containsString(ThumbnailControllerV3.URL_ERROR_MESSAGE)));

        this.mockMvc.perform(get(V3_ENDPOINT, "", ""))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void test_HeadEmptyPathVariables() throws Exception {

        this.mockMvc.perform(get(V3_ENDPOINT, 400, ""))
                .andExpect(status().is4xxClientError());


        this.mockMvc.perform(get(V3_ENDPOINT, "", TestData.URI_HASH))
                .andExpect(status().isBadRequest());


        this.mockMvc.perform(get(V3_ENDPOINT, "", ""))
                .andExpect(status().is4xxClientError());


    }
}
