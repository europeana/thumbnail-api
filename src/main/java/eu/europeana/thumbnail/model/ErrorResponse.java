package eu.europeana.thumbnail.model;

import java.util.List;

/**
 *  Error Response class
 *  Created by Srishti Singh
 */
public class ErrorResponse {

    private  String       message;
    private  List<String> details;

    /**
     * Constructor used for initialization
     * @param message
     * @param details
     */
    public ErrorResponse(String message, List<String> details) {
        super();
        this.message = message;
        this.details = details;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getDetails() {
        return details;
    }
}
