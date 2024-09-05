package eu.europeana.thumbnail.exception;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * Error thrown when a requested MediaFile cannot be found.
 */
public class ThumbnailNotFoundException extends EuropeanaApiException {
    public ThumbnailNotFoundException() {
        super("Media file not found");
    }

    @Override
    public boolean doLog() {
        return false;
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
