package eu.europeana.thumbnail.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;

/**
 * Base error class for this application
 * @author Srishti Singh
 * Created on 12-08-2019
 */
public class ThumbnailException extends EuropeanaApiException {

    public ThumbnailException(String msg, Throwable t) {
        super(msg, t);
    }

    public ThumbnailException(String msg) {
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
