package eu.europeana.thumbnail.service.exception;


import eu.europeana.thumbnail.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@ResponseBody
public class CustomExceptionHandler extends ResponseEntityExceptionHandler
    {
        private static final String BAD_REQUEST = "BAD_REQUEST";

        @ExceptionHandler(ConstraintViolationException.class)
        public final ResponseEntity<ErrorResponse> handleConstraintViolation(
                ConstraintViolationException ex,
                WebRequest request)
        {
            List<String> details = ex.getConstraintViolations()
                    .parallelStream()
                    .map(e -> e.getMessage())
                    .collect(Collectors.toList());

            ErrorResponse error = new ErrorResponse(BAD_REQUEST, details);
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

