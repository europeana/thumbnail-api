package eu.europeana.thumbnail.service.impl;

import eu.europeana.thumbnail.model.MediaFile;
import eu.europeana.thumbnail.service.MediaStorageService;
import eu.europeana.domain.ObjectMetadata;
import eu.europeana.domain.StorageObject;
import eu.europeana.features.ObjectStorageClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jclouds.io.Payload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 *  Service for retrieving media (e.g. thumbnails) from an object storage like Amazons S3 or IBM Cloud S3
 */

@Service
public class MediaStorageServiceImpl implements MediaStorageService {

        private static final Logger LOG = LogManager.getLogger(MediaStorageServiceImpl.class);

        @Autowired
        private ObjectStorageClient metisobjectStorageClient;
        @Autowired
        private ObjectStorageClient uimObjectStorageClient;

       /**
        * objectStorageClient value is decided based on the credentials to be loaded ie; Metis or UIM
        * @see eu.europeana.thumbnail.service.MediaStorageService#isMetis(boolean)
        */

        private ObjectStorageClient objectStorageClient;

        /**
         * @see eu.europeana.thumbnail.service.MediaStorageService#checkIfExists(String)
         */
        @Override
        public Boolean checkIfExists(String id) {

            return objectStorageClient.isAvailable(id);
        }

        /**
         * @see eu.europeana.thumbnail.service.MediaStorageService#retrieveAsMediaFile(String, String, boolean)
         */
        @Override
        public MediaFile retrieveAsMediaFile(String id, String originalUrl, boolean withContent) {
            StorageObject storageObject = retrieveAsStorageObject(id, withContent);
            if (storageObject == null) {
                return null;
            }

            byte[] content = null;
            if (withContent) {
                try {
                    content = convertPayloadToByteArray(storageObject.getPayload());
                } catch (IOException e) {
                    LOG.error("Error reading media file contents {}", id, e);
                }
            }

            return new MediaFile(storageObject.getName(),
                    originalUrl,
                    content,
                    storageObject.getMetadata());
        }

        /**
         * @see eu.europeana.thumbnail.service.MediaStorageService#retrieveAsStorageObject(String, boolean)
         */
        @Override
        public StorageObject retrieveAsStorageObject(String id, boolean withContent) {
            final Optional<StorageObject> optStorageObject = withContent ? objectStorageClient.get(id) :
                    objectStorageClient.getWithoutBody(id);

            if (!optStorageObject.isPresent()) {
                return null;
            }
            return optStorageObject.get();
        }

        /**
         * @see eu.europeana.thumbnail.service.MediaStorageService#convertPayloadToByteArray(Payload)
         */
        @Override
        public byte[] convertPayloadToByteArray(Payload payload) throws IOException {
            byte[] result = null;
            try (InputStream in = payload.openStream()) {
                result = IOUtils.toByteArray(in);
            }
            return result;
        }

        /**
         * @see eu.europeana.thumbnail.service.MediaStorageService#retrieveContent(String)
         */
        @Override
        public byte[] retrieveContent(String id) {return objectStorageClient.getContent(id); }

        /**
         * @see eu.europeana.thumbnail.service.MediaStorageService#retrieveMetaData(String)
         */
        @Override
        public ObjectMetadata retrieveMetaData(String id) {
            return objectStorageClient.getMetaData(id);
        }


    /**
         * @see eu.europeana.thumbnail.service.MediaStorageService#createOrModify(MediaFile)
         * @deprecated September 2018, no longer needed/required
         */
        @Deprecated
        @Override
        public void createOrModify(MediaFile mediaFile) {
            throw new NotImplementedException("Functionality was removed to avoid accidental usage");
            // keeping it temporarily, in case Metis Media Service needs it.

//        delete(mediaFile.getId());
//
//        final Payload payload = newByteArrayPayload(mediaFile.getContent());
//
//        payload.getContentMetadata().setContentType(mediaFile.getContentType());
//        payload.getContentMetadata().setContentLength(mediaFile.getLength());
//
//        ObjectMetadata metadata = new ObjectMetadata();
//
//        metadata.setContentType(mediaFile.getContentType());
//        metadata.setContentLength(mediaFile.getSize().longValue());
//
//        objectStorageClient.put(mediaFile.getId(), payload);
        }

        /**
         * @see eu.europeana.thumbnail.service.MediaStorageService#delete(String)
         * @deprecated September 2018, no longer needed/required
         */
        @Deprecated
        @Override
        public void delete(String id) {
            throw new NotImplementedException("Functionality was removed to avoid accidental usage");
            // keeping it temporarily, in case Metis Media Service needs it.
            //objectStorageClient.delete(id);
        }

        /**
         * @see eu.europeana.thumbnail.service.MediaStorageService#retrieveAsStorageObject(String, boolean)
         */
         @Override
         public ObjectStorageClient isMetis(boolean isMetis) {
         if(isMetis) {
            objectStorageClient=metisobjectStorageClient;
          }
         else {
            objectStorageClient=uimObjectStorageClient;
          }
         return objectStorageClient; }
         }