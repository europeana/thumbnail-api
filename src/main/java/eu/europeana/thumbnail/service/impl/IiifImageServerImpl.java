package eu.europeana.thumbnail.service.impl;

import eu.europeana.thumbnail.model.ImageSize;
import eu.europeana.thumbnail.model.MediaStream;
import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.thumbnail.utils.IiifUtils;
import jakarta.validation.constraints.Pattern;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Download a thumbnail image from the Europeana IIIF Image server
 * Note that this service doesn't support retrieving metadata or checking if a file exists in advance
 *
 * @author Patrick Ehlert
 * Created on 2 sep 2020
 */
@Validated
@Component
public class IiifImageServerImpl implements MediaStorageService {

    public static final String STORAGE_NAME = "IIIF-IS";

    private static final Logger LOG = LogManager.getLogger(IiifImageServerImpl.class);
    private static final String INVALID_URL_MESSAGE = "Not a valid url";

    @Override
    public Boolean checkIfExists(String id) {
        throw new NotImplementedException("Not implemented");
    }

    /**
     * @see MediaStorageService#retrieve(String, String)
     *
     * Note that this is only supported if the originalUrl is provided (so for v2 requests), otherwise we simply return null
     *
     * @param id          optional, the id of the file
     * @param originalUrl the original url
     * @return MediaStream if the file exists, otherwise null
     */
    @Override
    @SuppressWarnings("javasecurity:S5145") // we only log for debug purposes, plus we validate the user input
    public MediaStream retrieve(String id, String originalUrl) {
        LOG.debug("Retrieving file from IIIF image server with id {}, url = {}", id, originalUrl);
        if (StringUtils.isEmpty(originalUrl)) {
            LOG.debug("No originalUrl provided, skipping retrieval from IIIF Image server");
            return null;
        }

        String width = String.valueOf(ImageSize.LARGE.getWidth());
        if (id.endsWith(ImageSize.MEDIUM.name())) {
            width = String.valueOf(ImageSize.MEDIUM.getWidth());
        }
        String imageUrl = IiifUtils.getEuropeanaIiifThumbnailUrl(originalUrl, width);
        if (imageUrl == null) {
            LOG.debug("No Europeana IIIF image, skipping retrieval from IIIF Image server");
            return null;
        }

        InputStream content = retrieve(imageUrl);
        if (content == null) {
            return null;
        }
        return new MediaStream(id, imageUrl, content);
    }

    /**
     * Retrieve an image from the IIIF (eCloud) server.
     * Only for Thumbnail v2
     * @param originalUrl the "original url" (id) of the image to retrieve
     * @return inputstream to the image if available, otherwise null
     */
    public InputStream retrieve(
            @Pattern(regexp = "^(https?|ftp)://.*$", message = INVALID_URL_MESSAGE) String originalUrl) {
        try {
            return new BufferedInputStream(new URL(originalUrl).openStream());
        } catch (MalformedURLException e) {
            LOG.error("'{}' is not a valid url", originalUrl, e);
        } catch (IOException e) {
            LOG.error("Error reading image '{}' from IIIF image server", originalUrl, e);
        }
        return null;
    }

    @Override
    public String getName() {
        return STORAGE_NAME;
    }
}
