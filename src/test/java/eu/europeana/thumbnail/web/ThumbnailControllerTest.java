package eu.europeana.thumbnail.web;

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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for Thumbnail controller
 * @author Srishti Singh on 14-08-2019.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(ThumbnailController.class)
public class ThumbnailControllerTest {

    private static final String ORIG_IIIF_URL_HTTP = "http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001/full/full/0/default.jpg";
    private static final String REVISED_IIIF_URL_HTTP = "http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001/full/400,/0/default.jpg";

    // note that iiif currently doesn't support https, but we test it in case they add it
    private static final String ORIG_IIIF_URL_HTTPS = "https://IIIF.EUROPEANA.EU/records/NWGBII4G57XVAYLJYOFUJUFEIUS2G2BXLHVQT6QKRAWQVDA7ZRXA/representations/presentation_images/versions/9cb967b2-fcfd-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1920/10/10/1/19201010_1-0001/full/full/0/default.jpg";
    private static final String REVISED_IIIF_URL_HTTPS = "https://IIIF.EUROPEANA.EU/records/NWGBII4G57XVAYLJYOFUJUFEIUS2G2BXLHVQT6QKRAWQVDA7ZRXA/representations/presentation_images/versions/9cb967b2-fcfd-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1920/10/10/1/19201010_1-0001/full/200,/0/default.jpg";

    private static final String REGULAR_URL = "http://www.bildarchivaustria.at/Preview/15620341.jpg";

    private static final String URI = "test-thumbnail";
    private static final String DEFAULT_CONTENTLENGTH_IMAGE = "2319";
    private static final String DEFAULT_CONTENTLENGTH_VIDEO = "1932";
    private static final String UTF8_CHARSET = ";charset=UTF-8";


    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private MediaStorageServiceImpl thumbnailService;
    @MockBean
    private ObjectStorageClient objectStorage;

    /**
     * Tests if we detect IIIF image urls correctly
     */
    @Test
    public void DetectIiifUrlTest() {
        assertTrue(ThumbnailController.isIiifRecordUrl(ORIG_IIIF_URL_HTTP));
        assertTrue(ThumbnailController.isIiifRecordUrl(ORIG_IIIF_URL_HTTPS));
        assertFalse(ThumbnailController.isIiifRecordUrl(REGULAR_URL));
    }

    /**
     * Test if we generate IIIF image thumbnail urls correctly
     */
    @Test
    public void GetIiifThumbnailTest() throws URISyntaxException {
        assertEquals(REVISED_IIIF_URL_HTTP, ThumbnailController.getIiifThumbnailUrl(ORIG_IIIF_URL_HTTP, "400").toString());
        assertEquals(REVISED_IIIF_URL_HTTPS, ThumbnailController.getIiifThumbnailUrl(ORIG_IIIF_URL_HTTPS, "200").toString());
        assertNull(ThumbnailController.getIiifThumbnailUrl(REGULAR_URL, "400"));
        assertNull(ThumbnailController.getIiifThumbnailUrl(null, "300"));
    }

    /**
     * Test Get mapping
     */
    @Test
    public void testGetMapping() throws Exception {

        byte[] users = "test thumbnail image".getBytes();
        MediaFile mediaFile = new MediaFile(anyString(),URI,users);
        given(thumbnailService.retrieveAsMediaFile("test", any(), anyBoolean())).willReturn(mediaFile);
        given(objectStorage.getContent(anyString())).willReturn(users);

        this.mockMvc.perform(get("/api/v2/thumbnail-by-url.json").param("uri", URI))
                .andExpect(content().bytes(objectStorage.getContent(anyString())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_JPEG_VALUE)+UTF8_CHARSET))
                .andExpect(header().string("Content-Length", notNullValue()));
    }
    @Test
    public void testDefaultResponse() throws Exception {
        //for invalid Image and default icon
        this.mockMvc.perform(get("/api/v2/thumbnail-by-url.json").param("uri", URI))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_PNG_VALUE)+UTF8_CHARSET))
                .andExpect(header().string("Content-Length", DEFAULT_CONTENTLENGTH_IMAGE));

        //for invalid Video and default icon
        this.mockMvc.perform(get("/api/v2/thumbnail-by-url.json").param("uri", URI)
                .param("type", "VIDEO")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", (MediaType.IMAGE_PNG_VALUE)+UTF8_CHARSET))
                .andExpect(header().string("Content-Length", DEFAULT_CONTENTLENGTH_VIDEO));
    }

    /**
     * Test Head mapping
     */
    @Test
    public void testHeadMapping() throws Exception {
        //for invalid Image and default icon
        this.mockMvc.perform(head("/api/v2/thumbnail-by-url.json").param("uri", URI))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Length", "0"));

        //for invalid Video and default icon
        this.mockMvc.perform(head("/api/v2/thumbnail-by-url.json").param("uri", URI)
                .param("type", "VIDEO")).andExpect(status().isNotFound())
                .andExpect(header().string("Content-Length", "0"));
    }
}