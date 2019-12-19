package eu.europeana.thumbnail.web;

import eu.europeana.thumbnail.model.MediaFile;
import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.thumbnail.utils.ControllerUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Pattern;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Retrieves image thumbnails.
 * The thumbnail API doesn't require any form of authentication, providing an API key is optional.
 */
@RestController
@RequestMapping("/api")
@Validated
public class ThumbnailController {

    private static final Logger LOG = LogManager.getLogger(ThumbnailController.class);

    private static final String  IIIF_HOST_NAME      = "iiif.europeana.eu";
    private static final boolean LOG_DEBUG_ENABLED   = LOG.isDebugEnabled();
    private static final long    DURATION_CONVERTER  = 10000;
    private static final String  INVALID_URL_MESSAGE = "INVALID URL";

    private MediaStorageService metisobjectStorageClient;
    private MediaStorageService uimObjectStorageClient;

    /**
     * Instantiation of the objects
     *
     * @param metisobjectStorageClient object for Metis Storage
     * @param uimObjectStorageClient   object for UIM Storage
     */
    public ThumbnailController(MediaStorageService metisobjectStorageClient,
                               MediaStorageService uimObjectStorageClient) {
        this.metisobjectStorageClient = metisobjectStorageClient;
        this.uimObjectStorageClient = uimObjectStorageClient;
    }

    /**
     * Retrieves image thumbnails.
     *
     * @param url  optional, the URL of the media resource of which a thumbnail should be returned. Note that the URL should be encoded.
     *             When no url is provided a default thumbnail will be returned
     * @param size optional, the size of the thumbnail, can either be w200 (width 200) or w400 (width 400).
     * @param type optional, type of the default thumbnail (media image) in case the thumbnail does not exists or no url is provided,
     *             can be: IMAGE, SOUND, VIDEO, TEXT or 3D.
     * @return responsEntity
     */

    @GetMapping(value = "/v2/thumbnail-by-url.json")
    public ResponseEntity<byte[]> thumbnailByUrl(@RequestParam(value = "uri")
                                                 @Pattern(regexp = "^(https?|ftp)://.*$",
                                                          message = INVALID_URL_MESSAGE) String url,
                                                 @RequestParam(value = "size", required = false, defaultValue = "w400")
                                                         String size,
                                                 @RequestParam(value = "type", required = false, defaultValue = "IMAGE")
                                                         String type,
                                                 WebRequest webRequest,
                                                 HttpServletResponse response) {
        long                   startTime = 0;
        byte[]                 mediaContent;
        ResponseEntity<byte[]> result;
        final HttpHeaders      headers   = new HttpHeaders();

        if (LOG_DEBUG_ENABLED) {
            startTime = System.nanoTime();
            LOG.debug("Thumbnail url = {}, size = {}, type = {}", url, size, type);
        }

        MediaFile mediaFile = retrieveThumbnail(url, size);

        ControllerUtils.addResponseHeaders(response);
        // if there is no image, we return the default 'type' icon
        if (mediaFile == null) {
            headers.setContentType(MediaType.IMAGE_PNG);
            mediaContent = getDefaultThumbnailForNotFoundResourceByType(type);
            result = new ResponseEntity<>(mediaContent, headers, HttpStatus.OK);

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
                String width   = (StringUtils.equalsIgnoreCase(size, "w200") ? "200" : "400");
                URI    iiifUri = ThumbnailController.getIiifThumbnailUrl(url, width);
                if (iiifUri != null) {
                    LOG.debug("IIIF url = {} ", iiifUri.getPath());
                    mediaFile = downloadImage(iiifUri);
                }
            } catch (URISyntaxException e) {
                LOG.error("Error reading IIIF thumbnail url", e);
            } catch (IOException io) {
                LOG.error("Error retrieving IIIF thumbnail image {} ", url, io);
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
     * @deprecated since 2 Oct 2019. All the thumbnail are available in S3. This functionality s not present in V3.
     */
    @Deprecated(since="2.1", forRemoval=true)
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
     * @param width  desired image width
     * @return thumbnail URI for iiif urls, otherwise null
     * @throws URISyntaxException when the provided string is not a valid url
     * @deprecated since 2 Oct 2019. All the thumbnail are available in S3. This functionality s not present in V3.
     */
    @Deprecated(since="2.1", forRemoval=true)
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
     * @deprecated since 2 Oct 2019. All the thumbnail are available in S3. This functionality s not present in V3.
     */
    @Deprecated(since="2.1", forRemoval=true)
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

    /**
     * Retrieve the default thumbnail image as a byte array
     *
     * @param path
     * @return
     */
    private byte[] getImage(String path) {
        byte[] result = null;
        try (InputStream in = this.getClass().getResourceAsStream(path)) {
            result = IOUtils.toByteArray(in);
        } catch (IOException e) {
            LOG.error("Error reading default thumbnail file", e);
        }
        return result;
    }

    @SuppressWarnings("findsecbugs:WEAK_MESSAGE_DIGEST_MD5") // we have to use MD5, security is not an issue here
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

    private byte[] getDefaultThumbnailForNotFoundResourceByType(final String type) {
        switch (StringUtils.upperCase(type)) {
            case "IMAGE":
                return getImage("/images/EU_thumbnails_image.png");
            case "SOUND":
                return getImage("/images/EU_thumbnails_sound.png");
            case "VIDEO":
                return getImage("/images/EU_thumbnails_video.png");
            case "TEXT":
                return getImage("/images/EU_thumbnails_text.png");
            case "3D":
                return getImage("/images/EU_thumbnails_3d.png");
            default:
                return getImage("/images/EU_thumbnails_image.png");
        }

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
        return getMD5(resourceUrl) + "-" + (StringUtils.equalsIgnoreCase(resourceSize, "w200") ? "MEDIUM" : "LARGE");
    }
}
