package selector;

public class Fragment {
    private int minX;
    private int minY;

    private int maxX;
    private int maxY;

    private String label;

    public Fragment(int minX, int minY, int maxX, int maxY, String label) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.label = label;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public String getLabel() {
        return label;
    }
}
