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
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for Thumbnail controller
 * @author Srishti Singh on 05-09-2019.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(ThumbnailControllerV3.class)
public class ThumbnailControllerV3Test {

    private static final String ORIG_IIIF_URL_HTTP = "http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001/full/full/0/default.jpg";
    private static final String REVISED_IIIF_URL_HTTP = "http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001/full/400,/0/default.jpg";

    // note that iiif currently doesn't support https, but we test it in case they add it
    private static final String ORIG_IIIF_URL_HTTPS = "https://IIIF.EUROPEANA.EU/records/NWGBII4G57XVAYLJYOFUJUFEIUS2G2BXLHVQT6QKRAWQVDA7ZRXA/representations/presentation_images/versions/9cb967b2-fcfd-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1920/10/10/1/19201010_1-0001/full/full/0/default.jpg";
    private static final String REVISED_IIIF_URL_HTTPS = "https://IIIF.EUROPEANA.EU/records/NWGBII4G57XVAYLJYOFUJUFEIUS2G2BXLHVQT6QKRAWQVDA7ZRXA/representations/presentation_images/versions/9cb967b2-fcfd-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1920/10/10/1/19201010_1-0001/full/200,/0/default.jpg";

    private static final String REGULAR_URL = "http://www.bildarchivaustria.at/Preview/15620341.jpg";

    private static final String TEST_URL="test-v3-thumbnail";
    private static final long DEFAULT_CONTENTLENGTH= 2000L;
    //private static final String DEFAULT_CONTENTLENGTH_VIDEO = "1932";
    private static final String UTF8_CHARSET = ";charset=UTF-8";


    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private MediaStorageServiceImpl thumbnailService;
    @MockBean
    private ObjectStorageClient objectStorage;

    /**
     * Test Get mapping
     */
    @Test
    public void testGet_Head_200Ok_Response() throws Exception {

        byte[] users = "test thumbnail image".getBytes();
        MediaFile mediaFile = new MediaFile(anyString(),TEST_URL,users);
        given(thumbnailService.retrieveAsMediaFile("test", any(), anyBoolean())).willReturn(mediaFile);
        when(objectStorage.getContent(anyString())).thenReturn(users);

        this.mockMvc.perform(get("/thumbnail/v3/{size}/{url}",200,TEST_URL))
                .andExpect(content().bytes(objectStorage.getContent(anyString())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)+UTF8_CHARSET))
                .andExpect(header().string("Content-Length", notNullValue()));

        this.mockMvc.perform(head("/thumbnail/v3/{size}/{url}",200,TEST_URL))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)+UTF8_CHARSET))
                .andExpect(header().string("Content-Length", notNullValue()));

    }

    @Test
    public void test400Response() throws Exception {
        //For GET mapping
        //for invalid size value other than 200 or 400
        this.mockMvc.perform(get("/thumbnail/v3/{size}/{url}",456,TEST_URL))
                .andExpect(status().isNotFound());

        //for invalid url
        this.mockMvc.perform(get("/thumbnail/v3/{size}/{url}",200,TEST_URL))
                .andExpect(status().isNotFound());

        //For HEAD mapping
        //for invalid size value other than 200 or 400
        this.mockMvc.perform(head("/thumbnail/v3/{size}/{url}",456,TEST_URL))
                .andExpect(status().isNotFound());

        //for invalid url
        this.mockMvc.perform(head("/thumbnail/v3/{size}/{url}",200,TEST_URL))
                .andExpect(status().isNotFound());
    }
        }