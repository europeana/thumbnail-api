package eu.europeana.thumbnail.web;

import eu.europeana.thumbnail.config.StorageRoutes;
import eu.europeana.thumbnail.service.impl.LogoUploadService;
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

    private static final String ID_ERROR_MESSAGE = "Invalid or empty id";
    private static final String EMPTY_FILE_ERROR_MESSAGE = "Received file is empty";
    private static final String UNSUPPORTED_CONTENT_TYPE_ERROR_MESSAGE = "Unsupported content type";
    private static final String ERROR_PROCESSING_ERROR_MESSAGE = "Error processing image";

    private static final String[] SUPPORTED_IMAGE_TYPES = new String[]{"image/jpeg", "image/jpg", "image/png", "image/webp",
        "image/gif", "image/tiff", "image/bmp"};

    private final LogoUploadService logoUploadService;

    /**
     * Create a new UploadControler
     * @param storageRoutes autowired bean
     */
    @Autowired
    public UploadControllerV3(StorageRoutes storageRoutes) {
        this.logoUploadService = storageRoutes.getLogoUploadService();
        LOG.error("LogoUploadService = {}", this.logoUploadService);
    }

    /**
     * Provides method to upload images (organisation logos) to a separate storage. We'll create the medium and large
     * thumbnails for it and store it with the provided id.
     * @param id the identifier used to store the image
     * @param file the uploaded file
     * @return empty 406 response when succesful, or 401 when authorization fails, or 400 when there's a problem reading
     * the content, or 500 when there's a problem processing or storing the image.
     */
    // TODO Add token-based authorization
    @PutMapping(value = {"/v3/{id}", "/v3/{id}/", "/v3//{id}", "/v3//{id}/"})
    public ResponseEntity<String> uploadImageV3(
            @PathVariable(value = "id") @Pattern(regexp = "^[a-fA-F0-9]{8,128}$", message = ID_ERROR_MESSAGE) String id,
            @RequestParam("file") MultipartFile file) {
        long start =  System.currentTimeMillis();
        LOG.trace("Received upload PUT request with id {}", id);
        // Validate
        if (file == null || file.isEmpty()) {
            LOG.error(EMPTY_FILE_ERROR_MESSAGE + " id {}, name {}", id, (file == null ? null : file.getOriginalFilename()));
            return ResponseEntity.badRequest().body("Received empty file");
        }
        String mimeType = file.getContentType();
        if (mimeType == null || Arrays.stream(SUPPORTED_IMAGE_TYPES).noneMatch(mimeType::equalsIgnoreCase)) {
            LOG.error(UNSUPPORTED_CONTENT_TYPE_ERROR_MESSAGE + "id {}, name {}", id, file.getOriginalFilename());
            return ResponseEntity.badRequest().body(UNSUPPORTED_CONTENT_TYPE_ERROR_MESSAGE +
                    "\nSupported types are: " + Arrays.toString(SUPPORTED_IMAGE_TYPES));
        }

        try {
            this.logoUploadService.process(id, file);
            LOG.trace("Successfully uploaded image with id {} in {} ms", id, System.currentTimeMillis() - start);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            LOG.error(ERROR_PROCESSING_ERROR_MESSAGE + "id {}, name {}", id, file.getOriginalFilename(), e);
            return ResponseEntity.internalServerError().body(ERROR_PROCESSING_ERROR_MESSAGE + ":" + e.getMessage());
        }

    }

}
