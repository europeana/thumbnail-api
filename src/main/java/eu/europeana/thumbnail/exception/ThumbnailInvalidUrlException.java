package eu.europeana.thumbnail.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception class to catch invalid url exceptions
 * @author Srishti Singh
 * Created on 12-08-2019
 * Modified on 10-12-2020
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class ThumbnailInvalidUrlException extends EuropeanaApiException {

    public ThumbnailInvalidUrlException(String msg, Throwable t) {
        super(msg, t);
    }

    public ThumbnailInvalidUrlException(String msg) {
        super(msg);
    }

    /**
     * @return boolean indicating whether this type of exception should be logged or not
     */
    @Override
    public boolean doLog() {
        return true; // default we log all exceptions
    }

    /**
     * @return boolean indicating whether we should include the stacktrace in the logs (if doLog is enabled)
     */
    public boolean logStacktrace() {
        return true;
    }

}
