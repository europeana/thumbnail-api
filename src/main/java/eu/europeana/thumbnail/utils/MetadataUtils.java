package eu.europeana.thumbnail.utils;

import com.sksamuel.scrimage.metadata.ImageMetadata;
import com.sksamuel.scrimage.metadata.Tag;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Utility class for retrieving metadata
 */
public final class MetadataUtils {

    private MetadataUtils() {
        // empty constructor to avoid initialization
    }

    /**
     * @param stream with image data, note that this method will consume the stream!
     * @return list of Tag objects with metadata
     * @throws IOException when there's a problem reading the stream
     */
    public static List<Tag> getMetadata(InputStream stream) throws IOException {
        ImageMetadata metaData = ImageMetadata.fromStream(stream);
        return List.of(metaData.tags());
    }
}
