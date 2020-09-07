package eu.europeana.thumbnail.utils;

import java.util.Locale;

/**
 * Utilities regarding IIIF server and images
 *
 * @author Patrick Ehlert
 * Created on 2 sep 2020
 */
public class IiifUtils {

    private static final String  IIIF_HOST_NAME = "iiif.europeana.eu";

    private IiifUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * Check if the provided url is an image hosted on iiif.europeana.eu.
     *
     * @param url to the image
     * @return true if the provided url is an image hosted on iiif.europeana.eu, otherwise false
     */
    public static boolean isEuropeanaIiifUrl(String url) {
        if (url != null) {
            String urlLowercase = url.toLowerCase(Locale.GERMAN);
            return (urlLowercase.startsWith("http://" + IIIF_HOST_NAME) ||
                    urlLowercase.startsWith("https://" + IIIF_HOST_NAME));
        }
        return false;
    }

    /**
     * Convert an Europeana IIIF image url, to a static IIIF image with the requested size
     *
     * @param url    to a IIIF thumbnail hosted by Europeana
     * @param width  desired image width
     * @return String containing thumbnail url if image is Europeana IIIF image, otherwise null
     */
    public static String getEuropeanaIiifThumbnailUrl(String url, String width) {
        // all urls are encoded so they start with either http:// or https://
        // and end with /full/full/0/default.<extension>.
        if (isEuropeanaIiifUrl(url)) {
            return url.replace("/full/full/0/default.", "/full/" + width + ",/0/default.");
        }
        return null;
    }
}
