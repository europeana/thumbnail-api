package eu.europeana.thumbnail.config;

import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.thumbnail.service.StoragesService;
import eu.europeana.thumbnail.web.TestData;
import eu.europeana.thumbnail.web.ThumbnailControllerV2;
import eu.europeana.thumbnail.web.ThumbnailControllerV2Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMVC test to check if CORS is configured as desired
 */
@SuppressWarnings("java:S5786")
@WebMvcTest({ThumbnailControllerV2.class})
public class CorsConfigTest {

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
     * Test if CORS works for GET normal requests and error requests
     */
    @Test
    public void testCORSGet() throws Exception {
        // normal (200 response) request
        testNormalResponse(this.mockMvc.perform(get(ThumbnailControllerV2Test.V2_ENDPOINT)
                .param(ThumbnailControllerV2Test.URI_PARAMETER, TestData.URI)
                .header(HttpHeaders.ORIGIN, "https://test.com")));

        // error request
       testErrorResponse(this.mockMvc.perform(get(ThumbnailControllerV2Test.V2_ENDPOINT)
               .param(ThumbnailControllerV2Test.URI_PARAMETER, "xxx")
               .header(HttpHeaders.ORIGIN, "https://test.com")));
    }

    /**
     * Test if CORS works for HEAD normal requests and error requests
     */
    @Test
    public void testCORSHead() throws Exception {
        // normal (200 response) request
        testNormalResponse(this.mockMvc.perform(head(ThumbnailControllerV2Test.V2_ENDPOINT)
                .param(ThumbnailControllerV2Test.URI_PARAMETER, TestData.URI)
                .header(HttpHeaders.ORIGIN, "https://test.com")));

        // error request
        testErrorResponse(this.mockMvc.perform(head(ThumbnailControllerV2Test.V2_ENDPOINT)
                .param(ThumbnailControllerV2Test.URI_PARAMETER, "xxx")
                .header(HttpHeaders.ORIGIN, "https://test.com")));
    }

    /**
     * Test if CORS works for Options (Preflight) request
     */
    @Test
    public void testCORSOptions() throws Exception {
        // typical Europeana Portal request (with 200 response)
        testNormalResponse(mockMvc.perform(options("/myApi/{someRequest}", "123test")
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header(HttpHeaders.ACCEPT, "*/*")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-site")
                .header("Sec-Fetch-Dest", "empty")
                .header(HttpHeaders.REFERER, "https://www.europeana.eu/en/set/5")
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en-GB,en;q=0.9,nl;q=0.8")
                .header("dnt", "1")
                .header(HttpHeaders.ORIGIN, "https://test.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.AUTHORIZATION)
        ));
    }

    private void testNormalResponse(ResultActions actions) throws Exception {
        actions.andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
    }

    private void testErrorResponse(ResultActions actions) throws Exception {
        actions.andExpect(status().isBadRequest())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
    }

}
