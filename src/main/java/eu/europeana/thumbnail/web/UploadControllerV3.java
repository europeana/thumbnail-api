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

    protected static final String ID_ERROR_MESSAGE = "Invalid or empty idx";

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
    // TODO check if provided mime-type is acceptable format
    @PutMapping(value = {"/v3/{id}", "/v3/{id}/", "/v3//{id}", "/v3//{id}/"})
    public ResponseEntity<String> uploadImageV3(
            @PathVariable(value = "id") @Pattern(regexp = "^[a-fA-F0-9]{8,128}$", message = ID_ERROR_MESSAGE) String id,
            @RequestParam("file") MultipartFile file) {
        long start =  System.currentTimeMillis();
        LOG.trace("Received upload PUT request with id {}", id);
        // Validate
        if (file == null || file.isEmpty()) {
            LOG.error("Received empty file, name {}, id {}", (file == null ? null : file.getOriginalFilename()), id);
            return ResponseEntity.badRequest().build();
        }
        String mimeType = file.getContentType();
        if (mimeType == null || !mimeType.startsWith("image/")) {
            LOG.error("Sent file is not an image, name {}, id {}", file.getOriginalFilename(), id);
            return ResponseEntity.badRequest().build();
        }

        try {
            this.logoUploadService.process(id, file);
            LOG.trace("Successfully uploaded image with id {} in {} ms", id, System.currentTimeMillis() - start);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            LOG.error("Error processing file, name {}, id {}", file.getOriginalFilename(), id, e);
            return ResponseEntity.internalServerError().build();
        }

    }

}
