package eu.europeana.thumbnail.web;

import eu.europeana.thumbnail.model.ImageSize;
import eu.europeana.thumbnail.model.MediaFile;
import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.thumbnail.service.StoragesService;
import eu.europeana.thumbnail.utils.ControllerUtils;
import eu.europeana.thumbnail.utils.HashUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Basic functionality shared by both the V2 and V3 controller
 * @author Patrick Ehlert
 * Created on 2 sep 2020
 */
public abstract class AbstractController {

    private static final Logger LOG = LogManager.getLogger(AbstractController.class);
    private static final long DURATION_CONVERTER  = 1_000_000L;

    protected StoragesService storagesService;

    public AbstractController(StoragesService storagesService) {
        this.storagesService = storagesService;
    }

    /**
     * Retrieve a thumbnail from either the fileId or the combination of originalUrl and width
     * @param request the incoming request
     * @param fileId the id (MD5 hash of the file, if not provided it will be calculated)
     *               the Id should not already contain the desired width
     * @param originalUrl the original url of the image, this is used to determine the Content-Type of the response
     * @param width the requested with of the image, can be 200, 400
     * @return Optional containing the MediaFile, or an empty optional if the file cannot be retrieved
     */
    protected Optional<MediaFile> retrieveThumbnail(HttpServletRequest request, String fileId, String originalUrl, Integer width) {
        // calculate hash (if necessary)
        String id = fileId;
        if (StringUtils.isEmpty(fileId)) {
            id = computeId(originalUrl);
        }
        // add image width to id
        id = addWidth(id, width);

        String serverName = request.getServerName();
        if ("localhost".equalsIgnoreCase(serverName)) {
            // append port so we can test different routes locally
            serverName = serverName + ":" + request.getServerPort();
        }

        MediaFile result = null;
        List<MediaStorageService> mediaStorageServices = storagesService.getStorages(serverName);
        for (MediaStorageService mss : mediaStorageServices) {
            result = mss.retrieveAsMediaFile(id, originalUrl);
            if (result == null) {
                LOG.debug("File {} not present in storage {}", id, mss.getName());
            } else {
                LOG.debug("File {} found in storage {}", id, mss.getName());
                break;
            }
        }
        return Optional.ofNullable(result);
    }

    /**
     * Convert the provided url and size into a string representing the id of the media file. The id consists of the md5-
     * hash of the provided resourceUrl concatenated with a hyphen and a size (MEDIUM or LARGE)
     *
     * @param resourceUrl  url of the original image
     * @return id of the thumbnail as it is stored in S3 (but with width indication)
     */
    private String computeId(final String resourceUrl) {
        return HashUtils.getMD5(resourceUrl);
    }

    private String addWidth(final String id, final Integer resourceWidth) {
        String width = ImageSize.LARGE.name();
        if (resourceWidth != null && resourceWidth == ImageSize.MEDIUM.getWidth()) {
            width = ImageSize.MEDIUM.name();
        }
        return id + "-" + width;
    }

    /**
     * Set the proper response headers and return object
     * @param webRequest request that is handled
     * @param response HttpServletResponse that is to be returned
     * @param mediaFile the mediaFile that was found or null (if null a 404 is generated)
     * @return responseEntity (for 200 and 404 responses), or null (for 304 or 412 responses in which case reponse servlet is modified)
     */
    protected ResponseEntity<byte[]> generateResponse(WebRequest webRequest, HttpServletResponse response, @NonNull MediaFile mediaFile) {
        ControllerUtils.addDefaultResponseHeaders(response);

        // Normally we let Spring determine the Content-type based on the Accept headers in the request, but here we set
        // the type dynamically to either jpeg or png depending on the type of thumbnail that we retrieved.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(getMediaType(mediaFile.getOriginalUrl()));

        // Check if we should return the full response, or a 304
        // The check below automatically sets an ETag and last-Modified in our response header and returns a 304
        // but only when clients include the If_Modified_Since header in their request
        if (ControllerUtils.checkForNotModified(mediaFile, webRequest)) {
            return null;
        }
        // If “If-Match” is supplied, we check if the value is the same as the current “ETag” of the resource or if
        // it is “*”, if it's false we respond with HTTP 412
        if (ControllerUtils.checkForPrecondition(mediaFile, webRequest)) {
            response.setStatus(HttpStatus.PRECONDITION_FAILED.value());
            return null;
        }

        return new ResponseEntity<>(mediaFile.getContent(), headers, HttpStatus.OK);
    }

    /**
     * Return the media type of the image that we are returning. Note that we base our return value solely on the
     * extension in the original image url, so if that is not correct we could be returning the incorrect value
     *
     * @param url String
     * @return String containing the MediaType of the thumbnail image (png for PDF and PNG files, otherwise JPEG)
     */
    private MediaType getMediaType(String url) {
        if (url == null) {
            return MediaType.IMAGE_JPEG;
        }
        String urlLow = url.toLowerCase(Locale.GERMAN);
        if (urlLow.endsWith(".png") || urlLow.endsWith(".pdf")) {
            return MediaType.IMAGE_PNG;
        }
        return MediaType.IMAGE_JPEG;
    }

    /**
     * If DEBUG logging is enabled, this will log the duration of the request
     * @param startTime required, startTime of the request
     * @param requestInfo optional, additional request info to allow request tracing
     */
    protected void logRequestDuration(long startTime, String requestInfo) {
        if (LOG.isDebugEnabled()) {
            Long duration = (System.nanoTime() - startTime) / DURATION_CONVERTER;
            if (StringUtils.isBlank(requestInfo)) {
                LOG.debug("Processing time = {} ms ", duration);
            } else {
                LOG.debug("{}, processing time = {} ms ", requestInfo, duration);
            }
        }
    }

}
