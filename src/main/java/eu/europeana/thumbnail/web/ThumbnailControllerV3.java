package eu.europeana.thumbnail.web;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import eu.europeana.thumbnail.exception.ThumbnailInvalidUrlException;
import eu.europeana.thumbnail.exception.ThumbnailNotFoundException;
import eu.europeana.thumbnail.model.MediaFile;
import eu.europeana.thumbnail.service.StoragesService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Pattern;
import java.util.Optional;

/**
 * Retrieves image thumbnails for version 3
 * The thumbnail API doesn't require any form of authentication, providing an API key is optional.
 *
 * Note that the controller can return content in PNG and JPG format, depending on the format of the thumbnail that is
 * requested (extension parameter)
 */
@RestController
@RequestMapping("/thumbnail")
@Validated
public class ThumbnailControllerV3 extends AbstractController {

    private static final Logger LOG = LogManager.getLogger(ThumbnailControllerV3.class);

    protected static final String SIZE_ERROR_MESSAGE = "Invalid size. Supported values are 200 and 400";

    protected static final String ID_ERROR_MESSAGE = "Invalid or empty id";

    protected static final String URL_ERROR_MESSAGE = "Either Size or Id is missing. Correct url is /v3/{size}/{id}";

    public ThumbnailControllerV3(StoragesService storagesService) {
        super(storagesService);
    }

    /**
     * handles Invalid Urls like '/thumbnail/v3/200/', '/thumbnail/v3//xyz.jpg'
     * Returns 400 bad Request
     * @return responseEntity
     */
    @GetMapping(value = "/v3//{id}")
    public ResponseEntity<byte[]> thumbnailByUrlV3InvalidUrl() throws ThumbnailInvalidUrlException {
        throw new ThumbnailInvalidUrlException(URL_ERROR_MESSAGE);
    }

    /**
     * handles Invalid Urls like '/thumbnail/v3/', '/thumbnail/v3', '/thumbnail/v3//'
     * Returns 400 bad Request
     * @return responseEntity
     */
    @GetMapping(value = "/v3")
    public ResponseEntity<byte[]> thumbnailByUrlV3EmptyUrl() throws ThumbnailInvalidUrlException {
        throw new ThumbnailInvalidUrlException(URL_ERROR_MESSAGE);
    }

    /**
     * Retrieves image thumbnails.
     *
     * @param size, the size of the thumbnail, currently we support 200 (width 200px) or 400 (width 400px).
     * @param id, the id of the file (MD5 hash of original url)
     * @return responseEntity
     */
    @GetMapping(value = "/v3/{size}/{id}")
    public ResponseEntity<byte[]> thumbnailByUrlV3(
            @PathVariable(value = "size", required = false)
            @Pattern(regexp = "^(200|400)$", message = SIZE_ERROR_MESSAGE) String size,
            @PathVariable(value = "id") String id,
            WebRequest webRequest, HttpServletRequest request, HttpServletResponse response) throws EuropeanaApiException {
        long startTime = 0;
        if (LOG.isDebugEnabled()) {
            startTime = System.nanoTime();
            LOG.debug("Thumbnail id = {}, size = {}", id, size);
        }

        String idWithoutExtension = id;
        String extension = null;
        int i = id.lastIndexOf('.');
        // if there is no id present, return 400 Bad Request
        if (i == 0) {
           throw new ThumbnailInvalidUrlException(ID_ERROR_MESSAGE);
        }
        if (i > 0) {
            idWithoutExtension = id.substring(0, i);
            extension = id.substring(i);
            LOG.debug("Thumbnail cleaned id = {}, extension = {}", idWithoutExtension, extension);
        }

        Optional<MediaFile> mediaFile = retrieveThumbnail(request, idWithoutExtension, extension, Integer.valueOf(size));
        if (mediaFile.isEmpty()) {
            logRequestDuration(startTime, "Id = " + id + ", status = " + HttpStatus.NOT_FOUND);
            throw new ThumbnailNotFoundException();
        }

        ResponseEntity<byte[]> result = generateResponse(webRequest, response, mediaFile.get());
        logRequestDuration(startTime, "Id = " + id + ", status = " + response.getStatus());
        return result;
    }
}
