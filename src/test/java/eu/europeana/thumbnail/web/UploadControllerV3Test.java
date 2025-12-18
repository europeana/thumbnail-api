package eu.europeana.thumbnail.web;

import eu.europeana.thumbnail.config.ApiConfig;
import eu.europeana.thumbnail.config.StorageRoutes;
import eu.europeana.thumbnail.service.StoragesService;
import eu.europeana.thumbnail.service.UploadImageService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for Thumbnail V3 controller
 *
 * @author Srishti Singh on 05-09-2019.
 * @author Patrick Ehlert, major refactoring in September 2020
 */
@TestPropertySource("classpath:testroutes.properties")
@WebMvcTest({UploadControllerV3.class, StorageRoutes.class, ApiConfig.class})
@AutoConfigureMockMvc
@SuppressWarnings("java:S5786")
public class UploadControllerV3Test {

    private static final String ENDPOINT = "/thumbnail/v3/{id}";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private StoragesService storagesService;
    @MockitoBean
    private UploadImageService uploadImageService;

    MockMultipartFile textFile = new MockMultipartFile("file","hello.txt", MediaType.TEXT_PLAIN_VALUE,
            "Hello, World!".getBytes());
    MockMultipartFile fakeImageFile = new MockMultipartFile("file","hello.jpg", MediaType.IMAGE_JPEG_VALUE,
            "Hello, World!".getBytes());


    /**
     * Test normal 200 Ok requests
     */
    @Disabled
    // TODO fix unit test so it doesn't call the real uploadImageService.process() method.
    @Test
    public void test_200_Ok() throws Exception {
        doNothing().when(uploadImageService).process(anyString(), any());

        MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(ENDPOINT, "12345678");
        // we should do a PUT instead of a POST
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });
        this.mockMvc.perform(builder.file(fakeImageFile))
                .andExpect(status().isOk());
    }

    @Test
    public void test_400_InvalidId() throws Exception {
        MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(ENDPOINT, "invalid-id");
        // we should do a PUT instead of a POST
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });
        this.mockMvc.perform(builder.file(fakeImageFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(Matchers.containsString(UploadControllerV3.ID_ERROR_MESSAGE)));
    }

    @Test
    public void test_400_InvalidContentType() throws Exception {
        MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(ENDPOINT, "12345678");
        // we should do a PUT instead of a POST
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });
        this.mockMvc.perform(builder.file(textFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(Matchers.containsString(UploadControllerV3.UNSUPPORTED_CONTENT_TYPE_ERROR_MESSAGE)));
    }

}
