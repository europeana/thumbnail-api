package eu.europeana.thumbnail.utils;

import eu.europeana.thumbnail.model.ImageSize;
import org.apache.logging.log4j.LogManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generate S3 object ids using MD5 hash of image urls and requested size
 *
 * @author Patrick Ehlert
 * Created on 2 sep 2020
 */
public final class IdUtils {

    private IdUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * Return MD5 hash of the provided string (usually an image url)
     * @param resourceUrl url for which hash needs to generated
     * @return MD5 hash value
     */
    @SuppressWarnings("java:S4790")
    public static String getMD5(String resourceUrl){
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(resourceUrl.getBytes(StandardCharsets.UTF_8));
            final byte[] resultByte = messageDigest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte aResultByte : resultByte) {
                sb.append(Integer.toString((aResultByte & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            LogManager.getLogger().error("Could not find MD5 algorithm", e);
        }
        return "";
    }

    /**
     * In S3 we store 2 versions of each file, a medium and large version, each with their own postfix
     * This method returns the id of an individual file as stored in s3
     * @param id the received (partial) id in the request
     * @param resourceWidth in pixels (200 or 400). If a different width is provided we return the 400 pixel one
     * @return full id as used in S3 to store the file
     */
    public static String getS3ObjectId(final String id, final Integer resourceWidth) {
        String width = ImageSize.LARGE.name();
        if (resourceWidth != null && resourceWidth == ImageSize.MEDIUM.getWidth()) {
            width = ImageSize.MEDIUM.name();
        }
        return id + "-" + width;
    }

    /**
     * In S3 we store 2 versions of each file, a medium and large version, each with their own postfix
     * This method returns the id of an individual file as stored in s3
     * @param id the received (partial) id in the request
     * @param imageSize the size of the image
     * @return full id as used in S3 to store the file
     */
    public static String getS3ObjectId(final String id, final ImageSize imageSize) {
        return id + "-" + imageSize.name();
    }
}
