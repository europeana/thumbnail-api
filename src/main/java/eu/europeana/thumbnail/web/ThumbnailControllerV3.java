package eu.europeana.thumbnail.web;

import eu.europeana.thumbnail.model.MediaFile;
import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.thumbnail.utils.ControllerUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Retrieves image thumbnails for version 3
 * The thumbnail API doesn't require any form of authentication, providing an API key is optional.
 */
@RestController
@RequestMapping("/thumbnail")
public class ThumbnailControllerV3 {

    private static final Logger LOG = LogManager.getLogger(ThumbnailControllerV3.class);

    private static final String  IIIF_HOST_NAME     = "iiif.europeana.eu";
    private static final boolean LOG_DEBUG_ENABLED  = LOG.isDebugEnabled();
    private static final long    DURATION_CONVERTER = 10000;
    private static final int     WIDTH_200          = 200;
    private static final int     WIDTH_400          = 400;

    private MediaStorageService metisobjectStorageClient;
    private MediaStorageService uimObjectStorageClient;

    public ThumbnailControllerV3(MediaStorageService metisobjectStorageClient,
                                 MediaStorageService uimObjectStorageClient) {
        this.metisobjectStorageClient = metisobjectStorageClient;
        this.uimObjectStorageClient = uimObjectStorageClient;
    }

    /**
     * Retrieves image thumbnails.
     *
     * @param url  the URL of the media resource of which a thumbnail should be returned. Note that the URL should be encoded.
     * @param size , the size of the thumbnail, can either be 200 (width 200px) or 400 (width 400px).
     * @return responsEntity
     * @throws UnsupportedEncodingException the url is decoded in v3. Hence it may throw the exception.
     */

    @GetMapping(value = "/v3/{size}/{url}")
    public ResponseEntity<byte[]> thumbnailByUrl(@PathVariable(value = "size") int size,
                                                 @PathVariable("url") String url,
                                                 WebRequest webRequest,
                                                 HttpServletResponse response) throws UnsupportedEncodingException {

        String decodedURL = URLDecoder.decode(url, "UTF-8");
        long   startTime  = 0;
        if (LOG_DEBUG_ENABLED) {
            startTime = System.nanoTime();
            LOG.debug("Thumbnail url = {}, decodedURL = {} ,size = {}", url, decodedURL, size);
        }

        ControllerUtils.addResponseHeaders(response);
        byte[]                 mediaContent;
        ResponseEntity<byte[]> result;
        final HttpHeaders      headers = new HttpHeaders();

        //Check the “size” parameter, if it does not match either 200 or 400, respond with HTTP 404;
        if (size != WIDTH_200 && size != WIDTH_400) {
            mediaContent = null;
            result = new ResponseEntity<>(mediaContent, headers, HttpStatus.NOT_FOUND);
            if (LOG_DEBUG_ENABLED) {
                LOG.debug("The size entered is not valid size = {}", size);
            }
        } else {
            MediaFile mediaFile = retrieveThumbnail(decodedURL, String.valueOf(size));

            // if there is no image present in the storage,return 404 NOT FOUND with empty body
            if (mediaFile == null) {
                mediaContent = null;
                result = new ResponseEntity<>(mediaContent, headers, HttpStatus.NOT_FOUND);

            } else {
                headers.setContentType(getMediaType(decodedURL));
                mediaContent = mediaFile.getContent();
                result = new ResponseEntity<>(mediaContent, headers, HttpStatus.OK);

                // finally check if we should return the full response, or a 304
                // the check below automatically sets an ETag and last-Modified in our response header and returns a 304
                // (but only when clients include the If_Modified_Since header in their request)
                if (ControllerUtils.checkForNotModified(mediaFile, webRequest)) {
                    result = null;
                }
                // If “If-Match” is supplied, check if the value is the same as the current “ETag” of the resource or if it is “*”,
                // if false respond with HTTP 412;
                if (ControllerUtils.checkForPrecondition(mediaFile, webRequest)) {
                    result = new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
                }
            }
        }
        if (LOG_DEBUG_ENABLED) {
            Long duration = (System.nanoTime() - startTime) / DURATION_CONVERTER;
            if (MediaType.IMAGE_PNG.equals(headers.getContentType())) {
                LOG.debug("Total thumbnail request time (missing media): {}", duration);
            } else {
                if (result == null) {
                    LOG.debug("Total thumbnail request time (from s3 + return 304): {}", duration);
                } else {
                    LOG.debug("Total thumbnail request time (from s3 + return 200): {}", duration);
                }
            }
        }

        return result;
    }

    private MediaFile retrieveThumbnail(String url, String size) {

        MediaFile    mediaFile;
        final String mediaFileId = computeResourceUrl(url, size);
        LOG.debug("id = {}", mediaFileId);

        // 1. Check Metis storage first (IBM Cloud S3) because that has the newest thumbnails
        mediaFile = metisobjectStorageClient.retrieveAsMediaFile(mediaFileId, url, true);
        LOG.debug("Metis thumbnail = {}", mediaFile);

        // 2. Try the old UIM/CRF media storage (Amazon S3) second
        if (mediaFile == null) {
            mediaFile = uimObjectStorageClient.retrieveAsMediaFile(mediaFileId, url, true);
            LOG.debug("UIM thumbnail = {}", mediaFile);
        }

        // 3. We retrieve IIIF thumbnails by downloading a requested size from eCloud
        if (mediaFile == null && ThumbnailController.isIiifRecordUrl(url)) {
            try {
                String width   = (StringUtils.equalsIgnoreCase(size, "200") ? "200" : "400");
                URI    iiifUri = ThumbnailController.getIiifThumbnailUrl(url, width);
                if (iiifUri != null) {
                    LOG.debug("IIIF url = {} ", iiifUri.getPath());
                    mediaFile = downloadImage(iiifUri);
                }
            } catch (URISyntaxException e) {
                LOG.error("Error reading IIIF thumbnail url", e);
            } catch (IOException io) {
                LOG.error("Error retrieving IIIF thumbnail image", io);
            }
        }
        return mediaFile;
    }

    /**
     * Return the media type of the image that we are returning. Note that we base our return value solely on the
     * extension in the original image url, so if that is not correct we could be returning the incorrect value
     *
     * @param url String
     * @return MediaType of the thumbnail image (png for PDF and PNG files, otherwise JPEG)
     */
    private MediaType getMediaType(String url) {
        String urlLow = url.toLowerCase(Locale.GERMAN);
        if (urlLow.endsWith(".png") || urlLow.endsWith(".pdf")) {
            return MediaType.IMAGE_PNG;
        }
        return MediaType.IMAGE_JPEG;
    }

    /**
     * Check if the provided url is a thumbnail hosted on iiif.europeana.eu.
     *
     * @param url to a thumbnail
     * @return true if the provided url is a thumbnail hosted on iiif.europeana.eu, otherwise false
     */
    static boolean isIiifRecordUrl(String url) {
        if (url != null) {
            String urlLowercase = url.toLowerCase(Locale.GERMAN);
            return (urlLowercase.startsWith("http://" + IIIF_HOST_NAME) ||
                    urlLowercase.startsWith("https://" + IIIF_HOST_NAME));
        }
        return false;
    }

    /**
     * All 3 million IIIF newspaper thumbnails have not been processed yet in CRF (see also Jira EA-892) but the
     * edmPreview field will point to the default IIIF image url, so if we slightly alter that url the IIIF
     * API will generate a thumbnail in the appropriate size for us on-the-fly
     * Note that this is a temporary solution until all newspaper thumbnails are processed by CRF.
     *
     * @param url    to a IIIF thumbnail
     * @param width, desired image width
     * @return thumbnail URI for iiif urls, otherwise null
     * @throws URISyntaxException when the provided string is not a valid url
     */
    static URI getIiifThumbnailUrl(String url, String width) throws URISyntaxException {
        // all urls are encoded so they start with either http:// or https://
        // and end with /full/full/0/default.<extension>.
        if (isIiifRecordUrl(url)) {
            return new URI(url.replace("/full/full/0/default.", "/full/" + width + ",/0/default."));
        }
        return null;
    }

    /**
     * Download (IIIF) image from external location
     *
     * @param uri
     * @return
     * @throws IOException
     */
    private MediaFile downloadImage(URI uri) throws IOException {
        try (InputStream in = new BufferedInputStream(uri.toURL()
                                                         .openStream()); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int    n;
            while (-1 != (n = in.read(buf))) {
                out.write(buf, 0, n);
            }
            // for now we don't do anything with LastModified or ETag as this is not easily available for IIIF
            return new MediaFile(getMD5(uri.getPath()), uri.getPath(), out.toByteArray());
        }
    }

    @SuppressWarnings("squid:S2070") // we have to use MD5 here
    private String getMD5(String resourceUrl) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(resourceUrl.getBytes(StandardCharsets.UTF_8));
            final byte[]  resultByte = messageDigest.digest();
            StringBuilder sb         = new StringBuilder();
            for (byte aResultByte : resultByte) {
                sb.append(Integer.toString((aResultByte & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Error determining MD5 for resource {}", resourceUrl, e);
        }
        return resourceUrl;
    }

    /**
     * Convert the provided url and size into a string representing the id of the media file. The id consists of the md5-
     * hash of the provided resourceUrl concatenated with a hyphen and a size (MEDIUM or LARGE)
     *
     * @param resourceUrl  url of the original image
     * @param resourceSize requested thumbnail size (w200 or w400), default is w400 (meaning LARGE)
     * @return id of the thumbnail as it is stored in S3
     */
    private String computeResourceUrl(final String resourceUrl, final String resourceSize) {
        return getMD5(resourceUrl) + "-" + (StringUtils.equalsIgnoreCase(resourceSize, "200") ? "MEDIUM" : "LARGE");
    }
}

