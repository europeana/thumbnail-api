package eu.europeana.thumbnail.exception;

/**
 * Exception thrown during start-up when there's a problem in the configuration
 * @author Patrick Ehlert
 * Created on 1 sep 2020
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String msg) {
        super(msg);
    }
}
