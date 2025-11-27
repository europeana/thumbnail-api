package eu.europeana.thumbnail.exception;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * Error thrown when a requested MediaFile cannot be found.
 */
public class UploadAuthenticationException extends EuropeanaApiException {
    /**
     * Initialize a new ThumbnailNotFoundException (404)
     */
    public UploadAuthenticationException(String errorMessage) {
        super(errorMessage);
    }

    @Override
    public boolean doLog() {
        return false;
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
