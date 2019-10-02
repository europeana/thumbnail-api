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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
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

    private static final String GZIPSUFFIX     = "-gzip";
    private static final boolean LOG_DEBUG_ENABLED = LOG.isDebugEnabled();
    private static final long DURATION_CONVERTER=10000;
    private static final int WIDTH_200 = 200;
    private static final int WIDTH_400 = 400;
    private static final String INVALID_SIZE_MESSAGE = "The size should be 200 or 400.";
    private static final String INVALID_SIZE ="INVALID SIZE";

    private MediaStorageService metisobjectStorageClient;
    private MediaStorageService uimObjectStorageClient;

    public ThumbnailControllerV3(MediaStorageService metisobjectStorageClient, MediaStorageService uimObjectStorageClient) {
        this.metisobjectStorageClient = metisobjectStorageClient;
        this.uimObjectStorageClient = uimObjectStorageClient;
    }

    /**
     * Retrieves image thumbnails.
     * @param url  the URL of the media resource of which a thumbnail should be returned. Note that the URL is the hashed value with extension.
     * @param size , the size of the thumbnail, can either be 200 (width 200px) or 400 (width 400px).
     * @throws UnsupportedEncodingException the url is decoded in v3. Hence it may throw the exception.
     * @return responsEntity
     */

    @GetMapping(value = "/v3/{size}/{url}")
    public ResponseEntity<byte[]> thumbnailByUrl(
            @PathVariable(value= "size")  int size ,
            @PathVariable("url") String url,
            WebRequest webRequest, HttpServletResponse response) {

        String extensionRemoved = url.split("\\.")[0];
        long startTime = 0;
        if (LOG_DEBUG_ENABLED) {
            startTime = System.nanoTime();
            LOG.debug("Thumbnail url = {}, extensionRemoved = {} ,size = {}", url, extensionRemoved, size);
        }

        ControllerUtils.addResponseHeaders(response);
        byte[] mediaContent;
        ResponseEntity<byte[]> result;
        final HttpHeaders headers = new HttpHeaders();

        if(validateSize(size)) {
            List<String> details= new ArrayList<>();
            details.add(INVALID_SIZE_MESSAGE);
            result = new ResponseEntity(new ErrorResponse(INVALID_SIZE, details), headers, HttpStatus.NOT_FOUND);

        } else {
            MediaFile mediaFile = retrieveThumbnail(extensionRemoved, String.valueOf(size));

            // if there is no image present in the storage,return 404 NOT FOUND with empty body
            if (mediaFile == null) {
                mediaContent = null;
                result = new ResponseEntity<>(mediaContent, headers, HttpStatus.NOT_FOUND);

            } else {
                headers.setContentType(getMediaType(url));
                mediaContent = mediaFile.getContent();
                result = new ResponseEntity<>(mediaContent, headers, HttpStatus.OK);

                // finally check if we should return the full response, or a 304
                // the check below automatically sets an ETag and last-Modified in our response header and returns a 304
                // (but only when clients include the If_Modified_Since header in their request)
                if (checkForNotModified(mediaFile, webRequest)) {
                    result = null;
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

        MediaFile mediaFile;
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
     * Return the media type of the image that we are returning. Note that we base our return value solely on the
     * extension in the original image url, so if that is not correct we could be returning the incorrect value
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
     * Convert the provided url and size into a string representing the id of the media file. The id consists of the md5-
     * hash of the provided resourceUrl concatenated with a hyphen and a size (MEDIUM or LARGE)
     * @param resourceUrl url of the original image
     * @param resourceSize requested thumbnail size (w200 or w400), default is w400 (meaning LARGE)
     * @return id of the thumbnail as it is stored in S3
     */
    private String computeResourceUrl(final String resourceUrl, final String resourceSize) {
        return resourceUrl + "-" + (StringUtils.equalsIgnoreCase(resourceSize, "200") ? "MEDIUM" : "LARGE");
    }
    /** finally check if we should return the full response, or a 304
     * @param mediaFile
     * @param webRequest
     * @return boolean
     */
    private boolean checkForNotModified(MediaFile mediaFile, WebRequest webRequest) {

        if (mediaFile.getLastModified() != null && mediaFile.getETag() != null) {
            if (webRequest.checkNotModified(
                    StringUtils.removeEndIgnoreCase(mediaFile.getETag(), GZIPSUFFIX),
                    mediaFile.getLastModified().getMillis())) {
                return true;
            }
        } else if (mediaFile.getETag() != null && webRequest.checkNotModified(
                StringUtils.removeEndIgnoreCase(mediaFile.getETag(), GZIPSUFFIX))) {
            return true;
        }
        return false;

    }

    //validate the “size” parameter, if it does not match either 200 or 400, return true
    private boolean validateSize(int size) {
        if (String.valueOf(size) != null && size != WIDTH_200 && size != WIDTH_400) {
            if (LOG_DEBUG_ENABLED) {
                LOG.debug("The size entered is not valid size = {}", size);
            }
            return true;
        }
        return false;
    }
}

