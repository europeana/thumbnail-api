package eu.europeana.thumbnail.web;

import eu.europeana.thumbnail.model.ErrorResponse;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Retrieves image thumbnails for version 3
 * The thumbnail API doesn't require any form of authentication, providing an API key is optional.
 */
@RestController
@RequestMapping("/thumbnail")
public class ThumbnailControllerV3 {

    private static final Logger LOG = LogManager.getLogger(ThumbnailControllerV3.class);

    private static final boolean LOG_DEBUG_ENABLED         = LOG.isDebugEnabled();
    private static final long    DURATION_CONVERTER        = 10000;

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
    public ResponseEntity<byte[]> thumbnailByUrl(@PathVariable(value = "size") String size,
                                                 @PathVariable("url") String url,
                                                 WebRequest webRequest,
                                                 HttpServletResponse response) {

        long   startTime  = 0;
        String cleanedURL = getCleanedUrl(url);
        ControllerUtils.addResponseHeaders(response);
        ResponseEntity    result;
        final HttpHeaders headers = new HttpHeaders();
        int sizeValue;

        // converting size into integer
        try {
            sizeValue = Integer.parseInt(size);
        } catch (NumberFormatException e) {
            return sizeErrorMessage(size, headers);
        }

        if (LOG_DEBUG_ENABLED) {
            LOG.debug("Thumbnail url = {}, cleanedURL = {} , size = {}", url, cleanedURL, sizeValue);
        }

        //validating size
        if (sizeValue == 200 || sizeValue == 400){
            result = getThumbnail(cleanedURL, sizeValue, headers, webRequest);
        } else {
            return sizeErrorMessage(size, headers);
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

    /**
     * Retrieves image thumbnails for version 3
     * @param url     cleaned url of thumbnail
     * @param size    size if the image
     * @param headers headers for the response
     * @param webRequest
     * @return result
     */
    private ResponseEntity<byte[]> getThumbnail(String url, int size, HttpHeaders headers, WebRequest webRequest) {
        ResponseEntity    result;
        byte[]            mediaContent;
        MediaFile mediaFile = retrieveThumbnail(url, String.valueOf(size));

        // if there is no image present in the storage,return 404 NOT FOUND with empty body
        if (mediaFile == null) {
            result = new ResponseEntity<>(null, headers, HttpStatus.NOT_FOUND);

        } else {
            headers.setContentType(getMediaType(url));
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
        return mediaFile;
    }

    /**
     * checks the size passed in the request
     * @param wrongSize size passed
     * @param headers headers for the response
     */
    private ResponseEntity<byte[]> sizeErrorMessage(String wrongSize, HttpHeaders headers){
        String       message = String.format("'%s' is not a valid size parameter", wrongSize);
        List<String> details = new ArrayList<>();
        details.add("It must be either 200 or 400");
        return new ResponseEntity(new ErrorResponse(message, details), headers, HttpStatus.BAD_REQUEST);
    }

    /**
     * checks the size passed in the request
     * @param url url passed
     * @return decoded and cleaned url
     */
    private String getCleanedUrl(String url){
        String decodedURL = URLDecoder.decode(url, StandardCharsets.UTF_8);
        return decodedURL.split("\\.")[0];
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
     * Convert the provided url and size into a string representing the id of the media file. The id consists of the
     * md5-hash of the provided resourceUrl concatenated with a hyphen and a size (MEDIUM or LARGE)
     *
     * @param resourceUrl  url of the original image
     * @param resourceSize requested thumbnail size (w200 or w400), default is w400 (meaning LARGE)
     * @return id of the thumbnail as it is stored in S3
     */
    private String computeResourceUrl(final String resourceUrl, final String resourceSize) {
        return resourceUrl + "-" + (StringUtils.equalsIgnoreCase(resourceSize, "200") ? "MEDIUM" : "LARGE");
    }

}
