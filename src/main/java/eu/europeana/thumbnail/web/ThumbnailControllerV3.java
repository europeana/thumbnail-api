package eu.europeana.thumbnail.web;

import eu.europeana.thumbnail.config.StorageRoutes;
import eu.europeana.thumbnail.model.MediaFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Pattern;

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
public class ThumbnailControllerV3 extends BaseController {

    private static final Logger LOG = LogManager.getLogger(ThumbnailControllerV3.class);

    private static final String SIZE_ERROR_MESSAGE = "Invalid size. Supported values are 200 and 400";

    public ThumbnailControllerV3(StorageRoutes storageRoutes) {
        super(storageRoutes);
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
            @PathVariable(value = "size")
                @Pattern(regexp = "^(200|400)$", message = SIZE_ERROR_MESSAGE) String size,
            @PathVariable(value = "id") String id,
            WebRequest webRequest, HttpServletRequest request, HttpServletResponse response) {
        long startTime = 0;
        if (LOG.isDebugEnabled()) {
            startTime = System.nanoTime();
            LOG.debug("Thumbnail id = {}, size = {}", id, size);
        }

        String idWithoutExtension = id;
        String extension = null;
        int i = id.lastIndexOf('.');
        if (i > 0) {
            idWithoutExtension = id.substring(0, i);
            extension = id.substring(i);
            LOG.debug("Thumbnail cleaned id = {}, extension = {}", idWithoutExtension, extension);
        }

        MediaFile mediaFile = retrieveThumbnail(request, idWithoutExtension, extension, Integer.valueOf(size));
        ResponseEntity<byte[]> result = generateResponse(webRequest, response, mediaFile);

        logRequestDuration(startTime, "Id = " + id + ", status = " + response.getStatus());
        return result;
    }






}
