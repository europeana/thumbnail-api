package eu.europeana.thumbnail.exception;

import eu.europeana.api.commons.error.EuropeanaGlobalExceptionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Global exception handler that catches all errors and logs the interesting ones
 *
 * @author Srishti Singh
 * Created on 12-08-2019
 */
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler extends EuropeanaGlobalExceptionHandler {

    private static final Logger LOG         = LogManager.getLogger(GlobalExceptionHandler.class);

    /**
     * Checks if we should log an error and rethrows it
     *
     * @param e caught exception
     * @throws ThumbnailException rethrown exception
     */
    @ExceptionHandler(ThumbnailException.class)
    public void handleThumbnailException(ThumbnailException e) throws ThumbnailException {
        if (e.doLog()) {
            if (e.logStacktrace()) {
                LOG.error("Caught exception", e);
            } else {
                LOG.error("Caught exception: {}", e.getMessage());
            }
        }
        throw e;
    }
}
