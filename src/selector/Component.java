package selector;

public class Component {
    private final int pageWidth;
    private final int pageHeight;
    private final String pngName;
    private int minX;
    private int minY;

    private int maxX;
    private int maxY;

    private int classId;

    Component(int minX, int minY, int maxX, int maxY, int classId, int pageWidth, int pageHeight, String pngName) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.classId = classId;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.pngName = pngName;
    }

    int getMinX() {
        return minX;
    }

    int getMinY() {
        return minY;
    }

    int getMaxX() {
        return maxX;
    }

    int getMaxY() {
        return maxY;
    }

    int getClassId() {
        return classId;
    }

    public int getPageWidth() {
        return pageWidth;
    }

    public int getPageHeight() {
        return pageHeight;
    }

    public String getPngName() {
        return pngName;
    }
}
