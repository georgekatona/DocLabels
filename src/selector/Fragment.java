package selector;

public class Fragment {
    private double xOffset;
    private double yOffset;


    private double width;
    private double height;
    private String label;

    public Fragment(double xOffset, double yOffset, double width, double height, String label) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.width = width;
        this.height = height;
        this.label = label;
    }

    public double getxOffset() {
        return xOffset;
    }

    public double getyOffset() {
        return yOffset;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public String getLabel() {
        return label;
    }
}
