package org.zenika.handson.jigsaw.api;

import java.net.URL;

@SuppressWarnings("WeakerAccess")
public abstract class MarvelCharacter {
    public final int id;
    public final String name;
    public final String description;
    public final String imageId;
    public final String extension;

    public enum ImageType {
        LARGE("large"),
        PORTRAIT_XLARGE("portrait"),
        STANDARD_XLARGE("standard");


        public final String type;

        ImageType(String type) {
            this.type = type;
        }

        public static ImageType find(String type) {
            if (null == type || type.isEmpty())
                return ImageType.LARGE;
            for (ImageType imageType : ImageType.values()) {
                if(imageType.type.equalsIgnoreCase(type)) {
                    return imageType;
                }
            }

            return ImageType.LARGE;

        }

    }
    public MarvelCharacter(int id, String name, String description, String imageId, String extension) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageId = imageId;
        this.extension = extension;
    }

    public abstract URL getImage(ImageType type);


}
