package selector;

import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Controller {

    private static final String LABEL_OPTIONS_FILE = ".labels";

    private static final String[] LABEL_COLORS = {"green", "purple", "yellow", "blue", "red", "aqua", " fuchsia",
            "gray", "lime", "maroon", "navy", "olive", "silver", "teal"};

    private final Rectangle rect;

    private int xMin;
    private int xMax;
    private int yMin;
    private int yMax;

    private double imageWidth;
    private double imageHeight;

    private double ivWidth;
    private double ivHeight;

    private int gridWidth = 5;
    int[][] img;

    @FXML
    private AnchorPane parent;
    @FXML
    private ImageView imageToLabel;

    private int currentImageIndex;
    private List<File> images;
    private List<List<Fragment>> fragments = new ArrayList<>();

    private List<Stack<Fragment>> deletedFragments = new Stack<>();
    private List<String> labels;
    private ConnectedComponent prevComponent;
    private double rectStartX;
    private double rectStartY;
    private double rectEndX;
    private double rectEndY;

    public Controller() {
        labels = getLabelsFromFile();
        rect = new Rectangle(0, 0, 0, 0);
        rect.setStroke(Color.BLUE);
        rect.setStrokeWidth(1);
        rect.setStrokeLineCap(StrokeLineCap.ROUND);
        rect.setFill(Color.LIGHTBLUE.deriveColor(0, 1.2, 1, 0.6));
    }


    private List<String> getLabelsFromFile() {
        ArrayList<String> labels = new ArrayList<>();

        File file = new File(System.getProperty("user.dir") + File.separator + LABEL_OPTIONS_FILE);

        try {
            labels.addAll(Files.readAllLines(file.toPath(), Charset.defaultCharset()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return labels;
    }

    private void showImage(File file) {
        Image image = new Image(file.toURI().toString());
        imageWidth = image.getWidth();
        imageHeight = image.getHeight();
        imageToLabel.setImage(image);

        ivWidth = imageToLabel.getFitWidth();
        ivHeight = imageToLabel.getFitHeight();

        try {
            BufferedImage bi = ImageIO.read(images.get(currentImageIndex));
            img = convertToBoolArray(bi);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startRectSelection(MouseEvent mouseEvent) {
        rectStartX = mouseEvent.getX();
        rectStartY = mouseEvent.getY();

        rect.setX(rectStartX);
        rect.setY(rectStartY);
        rect.setWidth(0);
        rect.setHeight(0);

        parent.getChildren().add(rect);
    }

    public void onMouseDragged(MouseEvent mouseEvent) {
        double x = roundToImageHorizontalBounds(mouseEvent.getX());
        double y = roundToImageVerticalBounds(mouseEvent.getY());
        double offsetX = x - rectStartX;
        double offsetY = y - rectStartY;

        if (offsetX > 0)
            rect.setWidth(offsetX);
        else {
            rect.setX(x);
            rect.setWidth(rectStartX - rect.getX());
        }
        if (offsetY > 0) {
            rect.setHeight(offsetY);
        } else {
            rect.setY(y);
            rect.setHeight(rectStartY - rect.getY());
        }
    }

    public void endSelection(MouseEvent mouseEvent) {
        rectEndX = mouseEvent.getX();
        rectEndY = mouseEvent.getY();

        rectStartX = roundToImageHorizontalBounds(rectStartX);
        rectEndX = roundToImageHorizontalBounds(rectEndX);

        rectStartY = roundToImageVerticalBounds(rectStartY);
        rectEndY = roundToImageVerticalBounds(rectEndY);

        int x1 = (int) Math.round(getWidthPixelsFromCoordinate(rectStartX));
        int x2 = (int) Math.round(getWidthPixelsFromCoordinate(rectEndX));
        int y1 = (int) Math.round(getHeightPixelsFromCoordinate(rectStartY));
        int y2 = (int) Math.round(getHeightPixelsFromCoordinate(rectEndY));

        Fragment f = dropWhiteSpace(Math.min(x1, x2), Math.max(x1, x2),
                Math.min(y1, y2), Math.max(y1, y2));

        fragments.get(currentImageIndex).add(f);
        removeTempRect();
        updateView();
    }

    private void removeTempRect() {
        rect.setX(0);
        rect.setY(0);
        rect.setWidth(0);
        rect.setHeight(0);

        parent.getChildren().remove(rect);
    }

    private void showPopupMenu() {
        ContextMenu contextMenu = new ContextMenu();

        ArrayList<MenuItem> items = new ArrayList<>();
        for (String l : labels) {
            MenuItem item = new MenuItem(l);
            item.setOnAction(event -> selectLabel(l));
            items.add(item);
        }

        contextMenu.getItems().addAll(items);
        contextMenu.show(parent, convertShownX(xMax), convertShownY(yMax));
    }

    private void selectLabel(String label) {
        Fragment f = new Fragment(xMin, yMin,
                xMax, yMax, label);
        fragments.get(currentImageIndex).add(f);
        updateView();
        removeTempRect();
    }

    private void removeAllRects() {
        parent.getChildren().removeIf(p -> p instanceof Rectangle);
    }

    private void printFragments(List<Fragment> fragments) {
        for (Fragment f : fragments) {
            printFragment(f);
        }
    }

    private double roundToImageVerticalBounds(double y) {
        if (y < 0)
            return 0;
        double shownImageHeight = getShownImageHeight();
        if (y > shownImageHeight)
            return shownImageHeight;
        return y;
    }

    private double getShownImageHeight() {
        double aspectRatio = imageWidth / imageHeight;
        return Math.min(ivHeight, ivWidth / aspectRatio);
    }

    private double roundToImageHorizontalBounds(double x) {
        if (x < 0)
            return 0;
        double shownImageWidth = getShownImageWidth();
        if (x > shownImageWidth)
            return shownImageWidth;
        return x;
    }

    private double getShownImageWidth() {
        double aspectRatio = imageWidth / imageHeight;
        return Math.min(ivWidth, ivHeight * aspectRatio);
    }

    private void printFragment(Fragment f) {
        Rectangle finalRect = new Rectangle(convertShownX(f.getMinX()), convertShownY(f.getMinY()),
                convertShownX(f.getMaxX() - f.getMinX()), convertShownY(f.getMaxY() - f.getMinY()));

        switch (f.getLabel()) {
            case "TEXT":
                finalRect.setStroke(Color.BLACK);
                finalRect.setFill(Color.LIGHTGRAY.deriveColor(0, 1.2, 1, 0.6));
                break;
            case "FORMULA":
                finalRect.setStroke(Color.YELLOW);
                finalRect.setFill(Color.YELLOW.deriveColor(0, 1.2, 1, 0.6));
                break;
            case "TABLE":
                finalRect.setStroke(Color.PINK);
                finalRect.setFill(Color.LIGHTPINK.deriveColor(0, 1.2, 1, 0.6));
                break;
            case "REFERENCE":
                finalRect.setStroke(Color.DARKCYAN);
                finalRect.setFill(Color.CYAN.deriveColor(0, 1.2, 1, 0.6));
                break;
            case "FIGURE":
                finalRect.setStroke(Color.GREEN);
                finalRect.setFill(Color.LIGHTGREEN.deriveColor(0, 1.2, 1, 0.6));
                break;
            case "PICTURE":
                finalRect.setStroke(Color.DARKGREEN);
                finalRect.setFill(Color.GREEN.deriveColor(0, 1.2, 1, 0.5));
                break;
            case "CODE":
                finalRect.setStroke(Color.CYAN);
                finalRect.setFill(Color.LIGHTCYAN.deriveColor(0, 1.2, 1, 0.6));
                break;
            default:
                finalRect.setStroke(Color.BLACK);
                finalRect.setFill(Color.DARKGRAY.deriveColor(0, 1.2, 1, 0.2));
        }

        finalRect.setStrokeWidth(1);
        finalRect.setStrokeLineCap(StrokeLineCap.ROUND);
        parent.getChildren().add(finalRect);
    }

    private double convertShownX(int x) {
        return ((double) x) * getShownImageWidth() / imageWidth;
    }

    private double convertShownY(int y) {
        return ((double) y) * getShownImageHeight() / imageHeight;
    }

    private double getWidthPixelsFromCoordinate(double x) {
        return x * (imageWidth / getShownImageWidth());
    }

    private double getHeightPixelsFromCoordinate(double y) {
        return y * (imageHeight / getShownImageHeight());
    }

    public void saveFragments() {
        if (!isFragmentsEmpty()) {
            saveScript();
        }
    }

    private boolean isFragmentsEmpty() {
        for (List<Fragment> list : fragments) {
            if (!list.isEmpty())
                return false;
        }
        return true;
    }

    private void saveScript() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Script");
            File scriptFile = fileChooser.showSaveDialog(parent.getScene().getWindow());
            BufferedWriter writer = new BufferedWriter(new FileWriter(scriptFile));
            writer.write(getMaskScript());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getMaskScript() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < images.size(); i++) {
            Image image = new Image(images.get(i).toURI().toString());
            sb.append("convert -size ").append(image.getWidth()).append("x").append(image.getHeight())
                    .append(" xc:black ");

            for (int j = 0; j < fragments.get(i).size(); j++) {
                Fragment f = fragments.get(i).get(j);
                sb.append("-fill ")
                        .append(getFragmentColor(f))
                        .append(" -draw \"rectangle ")
                        .append(f.getMinX())
                        .append(",")
                        .append(f.getMinY())
                        .append(" ")
                        .append(f.getMaxX())
                        .append(",")
                        .append(f.getMaxY())
                        .append("\" ");
            }
            sb.append(getOutputFileName(i)).append("\n");
        }
        return sb.toString();
    }

    private String getFragmentColor(Fragment fragment) {

        int labelIndex = labels.indexOf(fragment.getLabel());

        if (labelIndex != -1 && labelIndex < LABEL_COLORS.length) {
            return LABEL_COLORS[labelIndex];
        } else {
            int r = Math.floorMod(fragment.getLabel().hashCode(), 256);
            int g = Math.floorMod(fragment.getLabel().hashCode() * 36, 256);
            int b = Math.floorMod(fragment.getLabel().hashCode() * 91, 256);

            return "\'rgb(" + r + "," + g + "," + b + ")\'";
        }
    }

    private String getOutputFileName(int fileIndex) {
        return removeFileExtension(images.get(fileIndex).getName()) + "_mask" + ".png";
    }

    private String removeFileExtension(String fileName) {
        if (fileName.indexOf(".") > 0) {
            return fileName.substring(0, fileName.lastIndexOf("."));
        } else {
            return fileName;
        }
    }

    public void openImages() {
        FileChooser fileChooser = new FileChooser();
        images = fileChooser.showOpenMultipleDialog(parent.getScene().getWindow());
        fragments.clear();
        deletedFragments.clear();
        removeAllRects();

        if (images.size() > 0) {
            currentImageIndex = 0;
            showImage(images.get(currentImageIndex));

            for (int i = 0; i < images.size(); i++) {
                fragments.add(new ArrayList<>());
                deletedFragments.add(new Stack<>());
            }
        }
    }

    public void prevImage() {
        currentImageIndex = Math.max(0, currentImageIndex - 1);
        if (images.size() > currentImageIndex) {
            showImage(images.get(currentImageIndex));

            updateView();
        }
    }

    public void nextImage() {
        currentImageIndex = Math.min(currentImageIndex + 1, images.size() - 1);
        if (images.size() > currentImageIndex) {
            showImage(images.get(currentImageIndex));

            updateView();
        }
    }

    public void undoSelection() {
        if (!fragments.get(currentImageIndex).isEmpty()) {
            deletedFragments.get(currentImageIndex).push(fragments.get(currentImageIndex).get(fragments.get(currentImageIndex).size() - 1));
            fragments.get(currentImageIndex).remove(fragments.get(currentImageIndex).size() - 1);
            updateView();
        }
    }

    public void redoSelection() {
        if (!deletedFragments.get(currentImageIndex).isEmpty()) {
            fragments.get(currentImageIndex).add(deletedFragments.get(currentImageIndex).pop());
            updateView();
        }
    }

    private void updateView() {
        removeAllRects();
        printFragments(fragments.get(currentImageIndex));
    }

    public void smartSelect(MouseEvent mouseEvent) {
        double imageX = getWidthPixelsFromCoordinate(mouseEvent.getX());
        double imageY = getHeightPixelsFromCoordinate(mouseEvent.getY());

        int gridIndexX = (int) (imageX / gridWidth);
        int gridIndexY = (int) (imageY / gridWidth);

        ConnectedComponent initialComponent;
        if (mouseEvent.isControlDown()) {
            initialComponent = prevComponent;
        } else {
            initialComponent = new ConnectedComponent(gridWidth);
        }
        ConnectedComponent component = getConnectedComponents(gridIndexX, gridIndexY, gridWidth, initialComponent);

        component = extendComponent(component);

        Fragment f = dropWhiteSpace(component.getMinX(), component.getMaxX(),
                component.getMinY(), component.getMaxY());

        fragments.get(currentImageIndex).add(f);
        updateView();

        prevComponent = component;
    }

    private Fragment dropWhiteSpace(int minX, int maxX, int minY, int maxY) {

        for (int i = minX; i < maxX; i++) {
            boolean onlyWhite = true;
            for (int j = minY; j < maxY; j++) {
                if (img[j][i] != 0xFFFFFFFF) {
                    onlyWhite = false;
                    break;
                }
            }
            if (onlyWhite) {
                minX = i + 1;
            } else {
                break;
            }
        }
        for (int i = maxX; i > minX; i--) {
            boolean onlyWhite = true;
            for (int j = minY; j < maxY; j++) {
                if (img[j][i] != 0xFFFFFFFF) {
                    onlyWhite = false;
                    break;
                }
            }
            if (onlyWhite) {
                maxX = i - 1;
            } else {
                break;
            }
        }
        for (int j = minY; j < maxY; j++) {
            boolean onlyWhite = true;
            for (int i = minX; i < maxX; i++) {
                if (img[j][i] != 0xFFFFFFFF) {
                    onlyWhite = false;
                    break;
                }
            }
            if (onlyWhite) {
                minY = j + 1;
            } else {
                break;
            }
        }
        for (int j = maxY; j > minY; j--) {
            boolean onlyWhite = true;
            for (int i = minX; i < maxX; i++) {
                if (img[j][i] != 0xFFFFFFFF) {
                    onlyWhite = false;
                    break;
                }
            }
            if (onlyWhite) {
                maxY = j - 1;
            } else {
                break;
            }
        }

        return new Fragment(minX, minY, maxX, maxY, labels.get(0));
    }

    private ConnectedComponent extendComponent(ConnectedComponent component) {
        // Make component rectangular
        int minX = component.getMinCellIndexX();
        int maxX = component.getMaxCellIndexX();
        int minY = component.getMinCellIndexY();
        int maxY = component.getMaxCellIndexY();
        boolean outOfRange = false;

        for (int i = minX; i < maxX + 1; i++) {
            for (int j = minY; j < maxY + 1; j++) {
                if (!component.contains(i, j)) {
                    for (Cell c : getConnectedComponents(i, j, gridWidth, component).getCells()) {
                        component.insertCell(c);
                        if (c.getIndexX() > maxX || c.getIndexX() < minX ||
                                c.getIndexY() > maxY || c.getIndexY() < minY) {
                            outOfRange = true;
                        }
                    }
                }
            }
        }
        if (outOfRange) {
            component = extendComponent(component);
        }
        return component;
    }

    private ConnectedComponent getConnectedComponents(int gridIndexX, int gridIndexY, int gridWidth,
                                                      ConnectedComponent component) {

        System.out.println("[" + gridIndexX + ";" + gridIndexY + "]");
        Cell c = new Cell(gridIndexX, gridIndexY);
        if (hasColoredPixel(c, gridWidth)) {
            component.insertCell(c);
            if (!component.contains(gridIndexX, gridIndexY - 1))
                component = getConnectedComponents(gridIndexX, gridIndexY - 1, gridWidth, component);
            if (!component.contains(gridIndexX + 1, gridIndexY))
                component = getConnectedComponents(gridIndexX + 1, gridIndexY, gridWidth, component);
            if (!component.contains(gridIndexX, gridIndexY + 1))
                component = getConnectedComponents(gridIndexX, gridIndexY + 1, gridWidth, component);
            if (!component.contains(gridIndexX - 1, gridIndexY))
                component = getConnectedComponents(gridIndexX - 1, gridIndexY, gridWidth, component);
        }
        return component;
    }

    private boolean hasColoredPixel(Cell c, int gridWidth) {
        for (int i = c.getIndexX() * gridWidth; i < (c.getIndexX() + 1) * gridWidth && i >= 0 && i < imageWidth; i++) {
            for (int j = c.getIndexY() * gridWidth; j < (c.getIndexY() + 1) * gridWidth && j >= 0 && j < imageHeight; j++) {
                if (img[j][i] != 0xFFFFFFFF) {
                    return true;
                }
            }
        }
        return false;
    }

    // Returns array of bools containing information if the pixel is white
    private static int[][] convertToBoolArray(BufferedImage image) {

        final int width = image.getWidth();
        final int height = image.getHeight();
        final int pixelLength;
        int[][] result = new int[height][width];

        final boolean hasAlphaChannel = image.getAlphaRaster() != null;
        if (hasAlphaChannel) {
            pixelLength = 4;
        } else {
            pixelLength = 3;
        }

        DataBuffer buffer = image.getRaster().getDataBuffer();
        if (buffer.getDataType() == DataBuffer.TYPE_BYTE) {
            final byte[] pixels = ((DataBufferByte) buffer).getData();
            for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
                int argb = 0;
                argb += -16777216; // 255 alpha
                argb += ((int) pixels[pixel] & 0xff); // blue
                argb += (((int) pixels[pixel + 1] & 0xff) << 8); // green
                argb += (((int) pixels[pixel + 2] & 0xff) << 16); // red
                result[row][col] = argb;
                col++;
                if (col == width) {
                    col = 0;
                    row++;
                }
            }
        } else {
            final short[] pixels = ((DataBufferUShort) buffer).getData();
            for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
                int argb = 0;
                argb += -16777216; // 255 alpha
                argb += ((int) pixels[pixel] & 0xff); // blue
                argb += (((int) pixels[pixel + 1] & 0xff) << 8); // green
                argb += (((int) pixels[pixel + 2] & 0xff) << 16); // red
                result[row][col] = argb;
                col++;
                if (col == width) {
                    col = 0;
                    row++;
                }
            }
        }
        return result;
    }

}
