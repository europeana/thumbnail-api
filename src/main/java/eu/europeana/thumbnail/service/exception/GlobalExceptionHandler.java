package eu.europeana.thumbnail.service.exception;

import eu.europeana.thumbnail.model.ErrorResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

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
    private static final String BAD_REQUEST = "BAD_REQUEST";

    /**
     * Checks if we should log an error and rethrows it
     *
     * @param e caught exception
     * @throws ThumbnailException rethrown exception
     */
    @ExceptionHandler(ThumbnailException.class)
    public void handleThumbnailException(ThumbnailException e) throws ThumbnailException {
        if (e.doLog()) {
            LOG.error("Caught exception", e);
        }
        throw e;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public final ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations()
                                 .parallelStream()
                                 .map(e -> e.getMessage())
                                 .collect(Collectors.toList());

        ErrorResponse error = new ErrorResponse(BAD_REQUEST, details);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
