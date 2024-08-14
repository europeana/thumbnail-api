package eu.europeana.thumbnail.utils;

import eu.europeana.thumbnail.model.MediaFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Class containing a number of useful controller utilities (mainly for setting headers)
 */
public final class ControllerUtils {

    private static final String ALLOWED    = "GET, HEAD";
    private static final String NOCACHE    = "no-cache";
    private static final String IFMATCH    = "If-Match";
    private static final String ANY        = "*";
    private static final String GZIPSUFFIX = "-gzip";

    private ControllerUtils() {
        // to avoid instantiating this class
    }

    /**
     * Add the 'UTF-8' character encoding to the response
     *
     * @param response The response to add the encoding and headers to
     */
    public static void addDefaultResponseHeaders(HttpServletResponse response) {
        response.addHeader(HttpHeaders.ALLOW, ALLOWED);
        response.addHeader(HttpHeaders.CACHE_CONTROL, NOCACHE);
    }

    /**
     * Supports multiple values in the "If-Match" header
     *
     * @param webRequest incoming WebRequest
     * @param mediaFile  mediaFile with requested eTag
     * @return boolean true IF ("If-Match" header is supplied AND
     * (contains matching eTag OR == "*") )
     * otherwise false
     */

    public static boolean checkForPrecondition(MediaFile mediaFile, WebRequest webRequest) {
        return (StringUtils.isNotBlank(webRequest.getHeader(IFMATCH)) &&
                (!doesAnyETagMatch(webRequest.getHeader(IFMATCH), mediaFile.getETag())));
    }

    /**
     * Checks if we should return the full response, or a 304
     *
     * @param mediaFile  input Media file to check
     * @param webRequest webrequest object
     * @return boolean   is modified yes / no
     */
    public static boolean checkForNotModified(MediaFile mediaFile, WebRequest webRequest) {
        if (mediaFile.getLastModified() != null && mediaFile.getETag() != null) {
            return webRequest.checkNotModified(StringUtils.removeEndIgnoreCase(mediaFile.getETag(), GZIPSUFFIX),
                                               mediaFile.getLastModified().getMillis());
        } else return mediaFile.getETag() != null &&
                      webRequest.checkNotModified(StringUtils.removeEndIgnoreCase(mediaFile.getETag(), GZIPSUFFIX));
    }

    private static boolean doesAnyETagMatch(String eTags, String eTagToMatch) {
        if (StringUtils.equals(ANY, eTags)) {
            return true;
        }
        if (StringUtils.isNoneBlank(eTags, eTagToMatch)) {
            for (String eTag : StringUtils.stripAll(StringUtils.split(eTags, ","))) {
                if (StringUtils.equalsIgnoreCase(spicAndSpan(eTag), spicAndSpan(eTagToMatch))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String spicAndSpan(String header) {
        return StringUtils.remove(StringUtils.stripStart(header, "W/"), "\"");
    }
}
