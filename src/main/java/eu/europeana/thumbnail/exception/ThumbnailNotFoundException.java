package eu.europeana.thumbnail.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Error thrown when a requested MediaFile cannot be found.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ThumbnailNotFoundException extends EuropeanaApiException {
    public ThumbnailNotFoundException() {
        super("Media file not found");
    }

    @Override
    public boolean doLog() {
        return false;
    }
}
