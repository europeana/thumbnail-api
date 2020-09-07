package eu.europeana.thumbnail.model;

/**
 * Enumeration of supported thumbnail image sizes (widths)
 * @author Patrick Ehlert
 * Created on 7 sep 2020
 */
public enum ImageSize {

    LARGE(400),
    MEDIUM(200);

    private int width;

    ImageSize(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }
}
