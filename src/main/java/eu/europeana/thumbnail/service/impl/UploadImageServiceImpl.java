package eu.europeana.thumbnail.service.impl;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.metadata.Tag;
import com.sksamuel.scrimage.webp.WebpWriter;
import eu.europeana.s3.S3ObjectStorageClient;
import eu.europeana.thumbnail.model.ImageSize;
import eu.europeana.thumbnail.service.UploadImageService;
import eu.europeana.thumbnail.utils.IdUtils;
import eu.europeana.thumbnail.utils.MetadataUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Service for reading an uploaded image (organisation logo), generating a 200 and 400 pixel version thumbnail and
 * storing this in an S3 storage
 */
public class UploadImageServiceImpl extends MediaReadStorageServiceImpl implements UploadImageService {

    private static final Logger LOG = LogManager.getLogger(UploadImageServiceImpl.class);

    /**
     * Initialize a new service for uploading images/logo's
     * @param storageName name of the used storage where files are stored
     * @param objectStorageClient client connected to the S3 object storage
     */
    public UploadImageServiceImpl(String storageName, S3ObjectStorageClient objectStorageClient) {
        super(storageName, objectStorageClient);
    }

    /**
     * Given an upload file, we generate the medium and small thumbnail and store it in S3
     * @param id the requested id
     * @param file the uploaded file
     * @throws IOException when there is and issue reading the uploaded file
     */
    public void process(String id, MultipartFile file) throws IOException {
        long startTime = System.currentTimeMillis();
        if (LOG.isDebugEnabled()) {
            List<Tag> metaData = MetadataUtils.getMetadata(file.getInputStream());
            StringBuilder sb = new StringBuilder();
            metaData.forEach(tag -> sb.append(tag).append("\n"));
            LOG.debug("Uploaded image metadata:\n{}", sb);
        }

        byte[] image = file.getBytes();
        generateThumbnailAndSave(id, image, ImageSize.LARGE);
        generateThumbnailAndSave(id, image, ImageSize.MEDIUM);
        LOG.info("Image with id {} and name {} processed successfully in {} ms", id, file.getOriginalFilename(),
                System.currentTimeMillis() - startTime);
    }

    private void generateThumbnailAndSave(String id, byte[] image, ImageSize size) throws IOException {
        LOG.debug("Generating {}px image for id {}...", size.getWidth(), id);
        try (InputStream convertedImage = ImmutableImage.loader().fromBytes(image)
                .scaleToWidth(size.getWidth())
                .forWriter(WebpWriter.DEFAULT).stream()) {
            String s3id = IdUtils.getS3ObjectId(id, size.getWidth());

            if (objectStorageClient.isObjectAvailable(s3id)) {
                LOG.warn("Replacing S3 object with id {}", s3id);
            }
            LOG.debug("Saving {}px image to S3...", size.getWidth());
            objectStorageClient.putObject(s3id, "image/webp", convertedImage);
        }
    }

}
