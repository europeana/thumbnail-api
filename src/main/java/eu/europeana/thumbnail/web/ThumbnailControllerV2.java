package eu.europeana.thumbnail.web;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.thumbnail.exception.ThumbnailNotFoundException;
import eu.europeana.thumbnail.model.ImageSize;
import eu.europeana.thumbnail.model.MediaFile;
import eu.europeana.thumbnail.service.StoragesService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Pattern;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Retrieves image thumbnails.
 * The Thumbnail API doesn't require any form of authentication, providing an API key is optional.
 *
 * Note that the controller can return content in PNG and JPG format, depending on the format of the thumbnail that is
 * requested (extension in url parameter)
 */
@RestController
@Validated
public class ThumbnailControllerV2 extends AbstractController {

    private static final Logger LOG = LogManager.getLogger(ThumbnailControllerV2.class);

    protected static final String  INVALID_URL_MESSAGE = "INVALID URL";

    public ThumbnailControllerV2(StoragesService storagesService) {
        super(storagesService);
    }

    /**
     * Retrieves image thumbnails.
     *
     * @param url  optional, the URL of the media resource of which a thumbnail should be returned. Note that the URL
     *             should be encoded. When no url is provided a default thumbnail will be returned
     * @param size optional, the size of the thumbnail, can either be w200 (width 200) or w400 (width 400).
     * @param type optional, type of the default thumbnail (media image) in case the thumbnail does not exists or no url is provided,
     *             can be: IMAGE, SOUND, VIDEO, TEXT or 3D.
     * @return responseEntity
     */
    @GetMapping(value = {"/api/v2/thumbnail-by-url.json", "/thumbnail/v2/url.json"})
    public ResponseEntity<byte[]> thumbnailByUrlV2(
            @RequestParam(value = "uri")
                @Pattern(regexp = "^(https?|ftp)://.*$", message = INVALID_URL_MESSAGE) String url,
            @RequestParam(value = "size", required = false, defaultValue = "w400") String size,
            @RequestParam(value = "type", required = false, defaultValue = "IMAGE") String type,
            WebRequest webRequest, HttpServletRequest request, HttpServletResponse response) throws EuropeanaApiException {
        long startTime = 0;
        if (LOG.isDebugEnabled()) {
            startTime = System.nanoTime();
            LOG.debug("Url = {}, size = {}, type = {}", url, size, type);
        }

        Optional<MediaFile> mediaFile = retrieveThumbnail(request, null, url, getWidth(size));
        ResponseEntity<byte[]> result;
        // if there is no image, we return the default 'type' icon
        if (mediaFile.isEmpty()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            result = new ResponseEntity<>(getDefaultThumbnailForNotFoundResourceByType(type), headers, HttpStatus.OK);
        } else {
            result = generateResponse(webRequest, response, mediaFile.get());
        }

        logRequestDuration(startTime, "Url = " + url + ", status = " + response.getStatus());
        return result;
    }

    private int getWidth(String size) {
        if (StringUtils.equalsIgnoreCase(size, "w200") || StringUtils.equalsIgnoreCase(size, "200")) {
            return ImageSize.MEDIUM.getWidth();
        }
        return ImageSize.LARGE.getWidth();
    }

    /**
     * Retrieve the default thumbnail image as a byte array
     *
     * @param path
     * @return
     */
    private byte[] getDefaultImage(String path) {
        byte[] result = null;
        try (InputStream in = this.getClass().getResourceAsStream(path)) {
            result = IOUtils.toByteArray(in);
        } catch (IOException e) {
            LOG.error("Error reading default thumbnail file", e);
        }
        return result;
    }

    private byte[] getDefaultThumbnailForNotFoundResourceByType(final String type) {
        String defaultImageName;
        switch (StringUtils.upperCase(type)) {
            case "IMAGE":
                defaultImageName = "EU_thumbnails_image.png";
                break;
            case "SOUND":
                defaultImageName = "EU_thumbnails_sound.png";
                break;
            case "VIDEO":
                defaultImageName = "EU_thumbnails_video.png";
                break;
            case "TEXT":
                defaultImageName = "EU_thumbnails_text.png";
                break;
            case "3D":
                defaultImageName = "EU_thumbnails_3d.png";
                break;
            default:
                defaultImageName = "EU_thumbnails_image.png";
        }
        return getDefaultImage("/images/" + defaultImageName);
    }


}
