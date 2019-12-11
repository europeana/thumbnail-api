package eu.europeana.thumbnail.web;

import eu.europeana.domain.ObjectMetadata;
import eu.europeana.features.ObjectStorageClient;
import eu.europeana.thumbnail.model.MediaFile;
import eu.europeana.thumbnail.service.impl.MediaStorageServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for Thumbnail controller
 *
 * @author Srishti Singh on 05-09-2019.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(ThumbnailControllerV3.class)
public class ThumbnailControllerV3Test {

    private static final String TEST_URL                = "test-v3-thumbnail";
    private static final String UTF8_CHARSET            = ";charset=UTF-8";
    private static final String THUMBNAIL_MEDIA_FILE    = "test thumbnail image";

    @Autowired
    private MockMvc                 mockMvc;
    @MockBean
    private MediaStorageServiceImpl thumbnailService;
    @MockBean
    private ObjectStorageClient     objectStorage;

    /**
     * Test Get mapping
     */
    @Test
    public void testGet_Head_200Ok_Response() throws Exception {
        byte[]    users     = THUMBNAIL_MEDIA_FILE.getBytes();
        MediaFile mediaFile = new MediaFile(anyString(), TEST_URL, users);
        given(thumbnailService.retrieveAsMediaFile("test", any(), anyBoolean())).willReturn(mediaFile);
        when(objectStorage.getContent(anyString())).thenReturn(users);

        this.mockMvc.perform(get("/thumbnail/v3/{size}/{url}", 200, TEST_URL))
                .andExpect(content().bytes(objectStorage.getContent(anyString())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", notNullValue()));

        this.mockMvc.perform(head("/thumbnail/v3/{size}/{url}", 200, TEST_URL))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(header().string("Content-Length", notNullValue()));
    }

    @Test
    public void test400Response() throws Exception {
        //For GET mapping
        //for invalid size value other than 200 or 400
        this.mockMvc.perform(get("/thumbnail/v3/{size}/{url}", 456, TEST_URL))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", (MediaType.APPLICATION_JSON_VALUE) + UTF8_CHARSET));

        //for invalid url
        this.mockMvc.perform(get("/thumbnail/v3/{size}/{url}", 200, TEST_URL)).andExpect(status().isNotFound());

        //For HEAD mapping
        //for invalid size value other than 200 or 400
        this.mockMvc.perform(head("/thumbnail/v3/{size}/{url}", 456, TEST_URL))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", (MediaType.APPLICATION_JSON_VALUE) + UTF8_CHARSET));


        //for invalid url
        this.mockMvc.perform(head("/thumbnail/v3/{size}/{url}", 200, TEST_URL)).andExpect(status().isNotFound());
    }

    @Test
    public void testGet_421_PreconditionFailedREsponse() throws Exception {
        Date                    dt  = new Date(2010, 3, 5, 0, 0);
        HashMap<String, Object> map = new HashMap<>();
        map.put("ETag", "12345abcde");
        map.put("Last-Modified", dt);
        ObjectMetadata metadata  = new ObjectMetadata(map);
        byte[]         users     = THUMBNAIL_MEDIA_FILE.getBytes();
        MediaFile      mediaFile = new MediaFile(anyString(), TEST_URL, users, metadata);

        given(thumbnailService.retrieveAsMediaFile("test", any(), anyBoolean())).willReturn(mediaFile);

        //for Valid ETag : *, 12345abcde
        this.mockMvc.perform(get("/thumbnail/v3/{size}/{url}", 200, TEST_URL).header("If-Match", "*"))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/thumbnail/v3/{size}/{url}", 200, TEST_URL).header("If-Match", "12345abcde"))
                .andExpect(status().isOk());

        //for invalid Etag : test
        this.mockMvc.perform(get("/thumbnail/v3/{size}/{url}", 200, TEST_URL).header("If-Match", "test"))
                .andExpect(status().isPreconditionFailed());
    }

}
