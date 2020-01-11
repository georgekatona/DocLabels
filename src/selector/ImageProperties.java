package selector;

class ImageProperties {
    private final int width;
    private final int height;
    private final String name;

    ImageProperties(int width, int height, String name) {
        this.width = width;
        this.height = height;
        this.name = name;

    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }


    String getName() {
        return name;
    }

    double getAspectRatio() {
        return (double) getWidth() / getHeight();
    }
}
