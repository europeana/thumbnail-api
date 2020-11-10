package eu.europeana.thumbnail.exception;

import eu.europeana.api.commons.error.EuropeanaGlobalExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
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
    // exception handling inherited from parent
}
