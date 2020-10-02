package eu.europeana.thumbnail.utils;

import org.apache.logging.log4j.LogManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generate image ids using MD5 hash of image urls
 *
 * @author Patrick Ehlert
 * Created on 2 sep 2020
 */
public final class HashUtils {

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
    @SuppressWarnings("findsecbugs:WEAK_MESSAGE_DIGEST_MD5") // we have to use MD5, security is not an issue here
    public static String getMD5(String resourceUrl) {
        messageDigest.reset();
        messageDigest.update(resourceUrl.getBytes(StandardCharsets.UTF_8));
        final byte[] resultByte = messageDigest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte aResultByte : resultByte) {
            sb.append(Integer.toString((aResultByte & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
}
