package eu.europeana.thumbnail.service.impl;

import eu.europeana.domain.ObjectMetadata;
import eu.europeana.thumbnail.model.ImageSize;
import eu.europeana.thumbnail.model.MediaFile;
import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.thumbnail.utils.IiifUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * Download a thumbnail image from the Europeana IIIF Image server
 * Note that this service doesn't support all the MediaStorageService features, only the retrieveAsMediaFile() method
 * and we use that slightly different than in the MediaStorageServiceImpl
 *
 * @author Patrick Ehlert
 * Created on 2 sep 2020
 */
public class IiifImageServerImpl implements MediaStorageService {

    public static final String STORAGE_NAME = "IIIF-IS";

    private static final Logger LOG = LogManager.getLogger(IiifImageServerImpl.class);

    @Override
    public Boolean checkIfExists(String id) {
        throw new NotImplementedException("Not implemented");
    }

    /**
     * @see MediaStorageService#retrieveAsMediaFile(String, String)
     *
     * Note that this is only supported if the originalUrl is provided (v2), otherwise we simply return null
     *
     * @param id          optional, the id of the file
     * @param originalUrl the original url
     * @return
     */
    @Override
    public MediaFile retrieveAsMediaFile(String id, String originalUrl) {
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

        byte[] content = retrieveContent(originalUrl);
        if (content != null && content.length > 0) {
            return new MediaFile(null, originalUrl, content);
        }
        return null;
    }

    @Override
    public byte[] retrieveContent(String originalUrl) {
        try (InputStream in = new BufferedInputStream(new URL(originalUrl).openStream());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int    n;
            while (-1 != (n = in.read(buf))) {
                out.write(buf, 0, n);
            }
            // for now we don't do anything with LastModified or ETag as this is not easily available for IIIF
            byte[] data = out.toByteArray();

            // verify that we got an image and not an error/redirect
            if (data.length == 0 || new String(data).toLowerCase(Locale.GERMAN).startsWith("<html>")) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("IIIF Image server returned error message: {}", String.valueOf(data));
                }
                return new byte[0];
            }
            return data;
        } catch (MalformedURLException e) {
            LOG.error("'{}' is not a valid url", originalUrl, e);
        } catch (IOException e) {
            LOG.error("Error reading image '{}' from IIIF image server", originalUrl, e);
        }
        return new byte[0];
    }

    @Override
    public ObjectMetadata retrieveMetaData(String id) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public String getName() {
        return STORAGE_NAME;
    }
}
