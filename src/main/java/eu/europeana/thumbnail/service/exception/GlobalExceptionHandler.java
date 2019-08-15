package eu.europeana.thumbnail.service.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler that catches all errors and logs the interesting ones
 * @author Srishti Singh
 * Created on 12-08-2019
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LogManager.getLogger(GlobalExceptionHandler.class);

    /**
     * Checks if we should log an error and rethrows it
     * @param e caught exception
     * @throws ThumbnailException rethrown exception
     */
    @ExceptionHandler(ThumbnailException.class)
    public void handleIiifException(ThumbnailException e) throws ThumbnailException {
        if (e.doLog()) {
            LOG.error("Caught exception", e);
        }
        throw e;
    }
}
