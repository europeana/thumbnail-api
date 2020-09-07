package eu.europeana.thumbnail.exception;

import eu.europeana.thumbnail.model.ErrorResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import java.nio.charset.StandardCharsets;

/**
 * Global exception handler that catches all errors and logs the interesting ones
 *
 * @author Srishti Singh
 * Created on 12-08-2019
 */
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

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

    @ExceptionHandler(ConstraintViolationException.class)
    public final ResponseEntity<ErrorResponse> handleConstraintViolation(HttpServletResponse response, ConstraintViolationException ex) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
