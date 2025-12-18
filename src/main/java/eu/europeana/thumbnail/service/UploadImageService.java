package eu.europeana.thumbnail.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Service for retrieving media (e.g. thumbnails) from an object storage like Amazons S3 or IBM Cloud S3
 */
public interface UploadImageService {

    /**
     * Given an upload file, we generate the medium and small thumbnail and store it in S3
     * The method is synchronous and will return once the processing is done
     * @param id the requested id
     * @param file the uploaded file
     * @throws IOException when there is and issue reading the uploaded file
     */
    public void process(String id, MultipartFile file) throws IOException;

}
