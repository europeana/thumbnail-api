package eu.europeana.thumbnail.utils;

import eu.europeana.thumbnail.exception.ThumbnailInvalidUrlException;
import eu.europeana.thumbnail.web.ThumbnailControllerV2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generate image ids using MD5 hash of image urls
 *
 * @author Patrick Ehlert
 * Created on 2 sep 2020
 */
@SuppressWarnings("findsecbugs:WEAK_MESSAGE_DIGEST_MD5") // we have to use MD5, security is not an issue here
public final class HashUtils {

    private static final Logger LOG = LogManager.getLogger(HashUtils.class);
    private static MessageDigest messageDigest;
    static {
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LogManager.getLogger().error("Could not find MD5 algorithm", e);
        }
    }

    private HashUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * Return MD5 hash of the provided string (usually an image url)
     * @param resourceUrl
     * @return
     */
    public static String getMD5(String resourceUrl) throws ThumbnailInvalidUrlException {
        try {
            messageDigest.reset();
            messageDigest.update(resourceUrl.getBytes(StandardCharsets.UTF_8));
            LOG.info("Message digest status state : {}. Calculating hash for {}", messageDigest.toString() , resourceUrl);
            final byte[] resultByte = messageDigest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte aResultByte : resultByte) {
                sb.append(Integer.toString((aResultByte & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            LOG.error("error calculating hash for {} with message digest : ", resourceUrl, messageDigest.toString());
            throw new ThumbnailInvalidUrlException(" Array out of bound index error" + resourceUrl);
        }
    }
}
