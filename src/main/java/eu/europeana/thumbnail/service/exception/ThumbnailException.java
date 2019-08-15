package eu.europeana.thumbnail.service.exception;

/**
 * Base error class for this application
 * @author Srishti Singh
 * Created on 12-08-2019
 */
public class ThumbnailException extends Exception {

    public ThumbnailException(String msg, Throwable t) {
        super(msg, t);
    }

    public ThumbnailException(String msg) {
        super(msg);
    }

    /**
     * @return boolean indicating whether this type of exception should be logged or not
     */
    public boolean doLog() {
        return true; // default we log all exceptions
    }

}
