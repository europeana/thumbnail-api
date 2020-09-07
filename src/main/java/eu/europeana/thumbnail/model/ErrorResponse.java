package eu.europeana.thumbnail.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.http.HttpStatus;

/**
 *  Error Response class
 *  Created by Srishti Singh
 */
@JsonPropertyOrder({"status", "error", "message"})
public class ErrorResponse {

    private  HttpStatus status;
    private  String message;

    /**
     * Constructor used for initialization
     * @param status
     * @param message
     */
    public ErrorResponse(HttpStatus status, String message) {
        super();
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status.value();
    }

    public String getError() {
        return status.getReasonPhrase();
    }

    public String getMessage() {
        return message;
    }


}
