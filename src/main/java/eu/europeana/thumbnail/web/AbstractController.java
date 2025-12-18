package eu.europeana.thumbnail.web;

import eu.europeana.thumbnail.model.MediaStream;
import eu.europeana.thumbnail.service.MediaReadStorageService;
import eu.europeana.thumbnail.service.StoragesService;
import eu.europeana.thumbnail.utils.ControllerUtils;
import eu.europeana.thumbnail.utils.IdUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Basic functionality shared by both the V2 and V3 controller
 * @author Patrick Ehlert
 * Created on 2 sep 2020
 */
@SuppressWarnings("javasecurity:S5145") // we only log for debug purposes, plus we validate the user input
public abstract class AbstractController {

    private static final Logger LOG = LogManager.getLogger(AbstractController.class);
    private static final long NANO_TO_MS  = 1_000_000L;

    protected StoragesService storagesService;

    protected AbstractController(StoragesService storagesService) {
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
    protected Optional<MediaStream> retrieveThumbnail(HttpServletRequest request, String fileId, String originalUrl, Integer width) {
        // calculate hash (if necessary)
        String id = fileId;
        if (StringUtils.isEmpty(fileId)) {
            id = computeId(originalUrl);
        }
        id = IdUtils.getS3ObjectId(id, width);

        String serverName = request.getServerName();
        if ("localhost".equalsIgnoreCase(serverName)) {
            // append port so we can test different routes locally
            serverName = serverName + ":" + request.getServerPort();
        }

        MediaStream result = null;
        List<MediaReadStorageService> mediaStorageServices = storagesService.getStorages(serverName);
        for (MediaReadStorageService mss : mediaStorageServices) {
            result = mss.retrieve(id, originalUrl);
            if (result == null) {
                LOG.debug("File {} not present in storage {}", id, mss.getName());
            } else {
                LOG.debug("File {} found in storage {}", id, mss.getName());
                // Temporarily added so we can get insight in how many images requested in production are not in IBM S3
                if ("uim-prod".equals(mss.getName())) {
                    // 2025-11-13 Temporarily changed to info level because should Amazon S3 migration is complete
                    // so this should not happen any more
                    LOG.warn("File with url {} and id {} found in old Amazon S3 storage", originalUrl, id);
                }
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
        return IdUtils.getMD5(resourceUrl);
    }



    /**
     * Set the proper response headers and return object
     * @param webRequest request that is handled
     * @param response HttpServletResponse that is to be returned
     * @param mediaFile the mediaFile that was found or null (if null a 404 is generated)
     * @return responseEntity (for 200 and 404 responses), or null (for 304 or 412 responses in which case reponse servlet is modified)
     */
    protected ResponseEntity<InputStreamResource> generateResponse(WebRequest webRequest, HttpServletResponse response, @NonNull MediaStream mediaFile) {
        ControllerUtils.addDefaultResponseHeaders(response);

        // Check if we should return the full response, or a 304
        // The check below automatically sets an ETag and last-Modified in our response header and returns a 304
        // but only when clients include the If_Modified_Since header in their request
        if (ControllerUtils.checkForNotModified(mediaFile, webRequest)) {
            mediaFile.close();
            return null;
        }
        // If “If-Match” is supplied, we check if the value is the same as the current “ETag” of the resource or if
        // it is “*”, if it's false we respond with HTTP 412
        if (ControllerUtils.checkForPrecondition(mediaFile, webRequest)) {
            mediaFile.close();
            response.setStatus(HttpStatus.PRECONDITION_FAILED.value());
            return null;
        }
        MediaType mediaType = this.getMediaType(mediaFile.getContentType(), mediaFile.getOriginalUrl());

        InputStreamResource imageStream = new InputStreamResource(mediaFile.getS3Object().inputStream());
        if (mediaFile.getContentLength() == null ) {
            LOG.warn("No content length for image with url {} and ETag {}", mediaFile.getOriginalUrl(), mediaFile.getETag());
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(imageStream);
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(mediaFile.getContentLength())
                .body(imageStream);
    }

    /**
     * Return the (likely) media type of the image that we are returning.
     *
     * Ideally we return the content type set in the object's metadata, but for "regular" thumbnails this is regularly set
     * to either "application/octet-stream" or "binary/octet-stream". Only for logo's that we store ourselves do we
     * return a have a proper content type in the metadata (image/webp).
     * This means that for regular thumbnails we'll try to guess the most likely type. Usually that's image/jpeg, but for
     * some v2 requests we know it's probably png.
     * This method is not 100% accurate but it works for our purposes, even if we get it wrong.
     *
     * @param contentTypeMetadata the content type as set in object's metadata (can be null)
     * @param url String
     * @return String containing the MediaType of the thumbnail image
     */
    private MediaType getMediaType(String contentTypeMetadata, String url) {
        MediaType result  = null;
        LOG.debug("ContentType from metadata {}", contentTypeMetadata);

        if (contentTypeMetadata != null && contentTypeMetadata.toLowerCase(Locale.getDefault()).startsWith("image/")) {
            result = MediaType.parseMediaType(contentTypeMetadata);
        }

        if (result == null) {
            // no proper content-type from metadata -> we have to guess
            if (url == null) {
                result = MediaType.IMAGE_JPEG; // v3 request -> regular thumbnails are generally stored as jpeg
            } else {
                // for v2 requests we can use the url to improve this; png and pdf files are usually stored as png
                String urlLow = url.toLowerCase(Locale.GERMAN);
                if (urlLow.endsWith(".png") || urlLow.endsWith(".pdf")) {
                    return MediaType.IMAGE_PNG;
                } else {
                    result = MediaType.IMAGE_JPEG;
                }
            }
            LOG.debug("ContentType from url: {}", result);
        }
        return result;
    }

    /**
     * If DEBUG logging is enabled, this will log the duration of the request
     * @param startTime required, startTime of the request
     * @param requestInfo optional, additional request info to allow request tracing
     */
    protected void logRequestDuration(long startTime, String requestInfo) {
        if (LOG.isDebugEnabled()) {
            Long duration = (System.nanoTime() - startTime) / NANO_TO_MS;
            if (StringUtils.isBlank(requestInfo)) {
                LOG.debug("Processing time = {} ms ", duration);
            } else {
                LOG.debug("{}, processing time = {} ms ", requestInfo, duration);
            }
        }
    }

}
