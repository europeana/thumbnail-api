package eu.europeana.thumbnail.web;

import eu.europeana.api.commons_sb3.definitions.oauth.Operations;
import eu.europeana.api.commons_sb3.error.exceptions.ApplicationAuthenticationException;
import eu.europeana.thumbnail.config.ApiConfig;
import eu.europeana.thumbnail.config.StorageRoutes;
import eu.europeana.thumbnail.service.UploadImageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;

/**
 * Offers image upload functionality
 * Uploaded images are converted to webp and saved in 200 pixel and 400 pixel wide thumbnails in a separate S3 bucket.
 * Retrieving those images can be done via the normal (V3) contoller.
 */
@RestController
@RequestMapping("/thumbnail")
@Validated
public class UploadControllerV3 {

    private static final Logger LOG = LogManager.getLogger(UploadControllerV3.class);

    protected static final String ID_ERROR_MESSAGE = "Invalid or empty id";
    protected static final String EMPTY_FILE_ERROR_MESSAGE = "Received file is empty";
    protected static final String UNSUPPORTED_CONTENT_TYPE_ERROR_MESSAGE = "Unsupported content type";
    protected static final String ERROR_PROCESSING_ERROR_MESSAGE = "Error processing image";

    private static final String[] SUPPORTED_IMAGE_TYPES = new String[]{"image/jpeg", "image/jpg", "image/png", "image/webp",
        "image/gif", "image/tiff", "image/bmp"};

    private final ApiConfig apiConfig;
    private final UploadImageService uploadImageService;

    /**
     * Create a new UploadControler
     * @param apiConfig autowired bean
     * @param storageRoutes autowired bean
     */
    @Autowired
    public UploadControllerV3(ApiConfig apiConfig, StorageRoutes storageRoutes) {
        this.apiConfig = apiConfig;
        this.uploadImageService = storageRoutes.getUploadImageService();
        if (this.uploadImageService == null) {
            LOG.info("Uploading is disabled");
        } else {
            LOG.warn("Uploading is enabled {}",
                    apiConfig.isUploadAuthEnabled() ? "with authorization" : "without authorization!");
        }
    }

    /**
     * Provides method to upload images (organisation logos) to a separate storage. We'll create the medium and large
     * thumbnails for it and store it with the provided id.
     * @param id the identifier used to store the image
     * @param file the uploaded file
     * @param request the received upload request
     * @return empty 406 response when succesful, or 401 when authorization fails, or 400 when there's a problem reading
     * the content, or 500 when there's a problem processing or storing the image.
     */
    @PutMapping(value = {"/v3/{id}", "/v3/{id}/", "/v3//{id}", "/v3//{id}/"})
    public ResponseEntity<String> uploadImageV3(
            @PathVariable(value = "id") @Pattern(regexp = "^[a-fA-F0-9]{8,128}$", message = ID_ERROR_MESSAGE) String id,
            @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        long start = System.currentTimeMillis();
        try {
            apiConfig.authorizeWriteAccess(request, Operations.UPDATE);
        } catch (ApplicationAuthenticationException e) {
            // TODO for some reason the GlobalExceptionHandler is not catching this. So as workaround we catch, log and rethrow
            LOG.error("Failed to authorize write access", e);
            throw new RuntimeException(e);
        }


        LOG.trace("Received upload PUT request with id {}", id);
        // Validate
        if (file == null || file.isEmpty()) {
            LOG.error(EMPTY_FILE_ERROR_MESSAGE + " id {}, name {}", id, (file == null ? null : file.getOriginalFilename()));
            return ResponseEntity.badRequest().body("Received empty file");
        }
        String contentType = file.getContentType();
        // filter out extra data such as ";UTF-8"
        if (contentType != null && contentType.contains(";")) {
            contentType = contentType.substring(0, contentType.indexOf(";"));
        }
        if (contentType == null || Arrays.stream(SUPPORTED_IMAGE_TYPES).noneMatch(contentType::equalsIgnoreCase)) {
            LOG.error(UNSUPPORTED_CONTENT_TYPE_ERROR_MESSAGE + " {}, id {}, name {}", contentType, id, file.getOriginalFilename());
            return ResponseEntity.badRequest().body(UNSUPPORTED_CONTENT_TYPE_ERROR_MESSAGE + ": " + contentType +
                    "\nSupported types are: " + Arrays.toString(SUPPORTED_IMAGE_TYPES));
        }

        try {
            this.uploadImageService.process(id, file);
            LOG.trace("Successfully uploaded image with id {} in {} ms", id, System.currentTimeMillis() - start);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            LOG.error(ERROR_PROCESSING_ERROR_MESSAGE + "id {}, name {}", id, file.getOriginalFilename(), e);
            return ResponseEntity.internalServerError().body(ERROR_PROCESSING_ERROR_MESSAGE + ":" + e.getMessage());
        }

    }

}
