package eu.europeana.thumbnail.utils;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Test class for IiifUtils
 *
 * @author Srishti Singh on 14-08-2019.
 * @author Patrick Ehlert, major refactoring in September 2020
 */
public class IiifUtilsTest {

    private static final String ORIG_IIIF_URL_HTTP    = "http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001/full/full/0/default.jpg";
    private static final String REVISED_IIIF_URL_HTTP = "http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001/full/400,/0/default.jpg";

    // note that the Europeana IIIF image server currently doesn't support https, but we test it in case they add it
    private static final String ORIG_IIIF_URL_HTTPS    = "https://IIIF.EUROPEANA.EU/records/NWGBII4G57XVAYLJYOFUJUFEIUS2G2BXLHVQT6QKRAWQVDA7ZRXA/representations/presentation_images/versions/9cb967b2-fcfd-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1920/10/10/1/19201010_1-0001/full/full/0/default.jpg";
    private static final String REVISED_IIIF_URL_HTTPS = "https://IIIF.EUROPEANA.EU/records/NWGBII4G57XVAYLJYOFUJUFEIUS2G2BXLHVQT6QKRAWQVDA7ZRXA/representations/presentation_images/versions/9cb967b2-fcfd-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1920/10/10/1/19201010_1-0001/full/200,/0/default.jpg";

    private static final String REGULAR_URL = "http://www.bildarchivaustria.at/Preview/15620341.jpg";

    /**
     * Tests if we detect Europeana IIIF image urls correctly
     */
    @Test
    public void testIsEuropeanaIiifUrl() {
        assertTrue(IiifUtils.isEuropeanaIiifUrl(ORIG_IIIF_URL_HTTP));
        assertTrue(IiifUtils.isEuropeanaIiifUrl(ORIG_IIIF_URL_HTTPS));
        assertFalse(IiifUtils.isEuropeanaIiifUrl(REGULAR_URL));
        assertFalse(IiifUtils.isEuropeanaIiifUrl(null));
    }

    /**
     * Test if we generate Europeana IIIF image thumbnail urls correctly
     */
    @Test
    public void GetIiifThumbnailTest() {
        assertEquals(REVISED_IIIF_URL_HTTP, IiifUtils.getEuropeanaIiifThumbnailUrl(ORIG_IIIF_URL_HTTP, "400"));
        assertEquals(REVISED_IIIF_URL_HTTPS, IiifUtils.getEuropeanaIiifThumbnailUrl(ORIG_IIIF_URL_HTTPS, "200"));
        assertNull(IiifUtils.getEuropeanaIiifThumbnailUrl(REGULAR_URL, "400"));
        assertNull(IiifUtils.getEuropeanaIiifThumbnailUrl(null, "300"));
    }
}
