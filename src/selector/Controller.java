package selector;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.FileChooser;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

public class Controller {

    @FXML
    private AnchorPane parent;
    @FXML
    private ImageView imageView;

    public Spinner<Integer> gridWidthSpinner;
    public Spinner<Integer> classSpinner;
    public CheckBox strapCheckBox;
    public CheckBox linesCheckBox;

    private ImageProperties imgProperties;
    private static final Color[] RECT_COLORS = {Color.DARKCYAN, Color.GREEN, Color.CYAN,
            Color.BLACK, Color.YELLOW, Color.PINK};

    private Rectangle tempRect;
    private Line lineX;
    private Line lineY;

    private List<File> images = new ArrayList<>();
    private int currentImageIndex = 0;
    private boolean[][] isImgWhite;
    private List<List<Component>> components = new ArrayList<>();
    private List<Stack<Component>> deletedComponents = new Stack<>();

    private int prevGridWidth = 5;
    private ConnectedCells prevComponentCells = new ConnectedCells(5);
    private Point2D.Double rectStartCoordinates;

    private boolean isDragSelectionActive;

    public Controller() {
        initDrawables();
    }

    private void initDrawables() {
        initRect();
        initLines();
    }

    private void initRect() {
        tempRect = new Rectangle(0, 0, 0, 0);
        tempRect.setMouseTransparent(true);
        tempRect.setStroke(Color.BLUE);
        tempRect.setStrokeWidth(1);
        tempRect.setStrokeLineCap(StrokeLineCap.ROUND);
        tempRect.setFill(Color.LIGHTBLUE.deriveColor(0, 1.2, 1, 0.6));
    }

    private void initLines() {
        lineX = new Line();
        lineY = new Line();
        lineX.setStrokeWidth(1);
        lineY.setStrokeWidth(1);
        lineX.setMouseTransparent(true);
        lineY.setMouseTransparent(true);
        lineX.setVisible(false);
        lineY.setVisible(false);
    }

    private void showImage(File file) {
        prevComponentCells = new ConnectedCells(gridWidthSpinner.getValue());
        Image image = new Image(file.toURI().toString());
        imageView.setImage(image);
        imgProperties = new ImageProperties((int) image.getWidth(), (int) image.getHeight(), file.getName());
        try {
            BufferedImage bi = ImageIO.read(images.get(currentImageIndex));
            isImgWhite = convertToBoolArray(bi);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void onMousePressed(MouseEvent mouseEvent) {
        if (!mouseEvent.isPrimaryButtonDown()) {
            return;
        }
        isDragSelectionActive = true;

        rectStartCoordinates = new Point.Double(mouseEvent.getX(), mouseEvent.getY());
        tempRect.setX(rectStartCoordinates.x);
        tempRect.setY(rectStartCoordinates.y);
        tempRect.setWidth(0);
        tempRect.setHeight(0);

        if (!parent.getChildren().contains(tempRect))
            parent.getChildren().add(tempRect);
    }

    public void onMouseDragged(MouseEvent mouseEvent) {
        double x = roundToImageHorizontalBounds(mouseEvent.getX());
        double y = roundToImageVerticalBounds(mouseEvent.getY());
        double offsetX = x - rectStartCoordinates.x;
        double offsetY = y - rectStartCoordinates.y;
        drawTempRect(x, y, offsetX, offsetY);
    }

    private void drawTempRect(double x, double y, double offsetX, double offsetY) {
        if (offsetX > 0)
            tempRect.setWidth(offsetX);
        else {
            tempRect.setX(x);
            tempRect.setWidth(rectStartCoordinates.x - tempRect.getX());
        }
        if (offsetY > 0) {
            tempRect.setHeight(offsetY);
        } else {
            tempRect.setY(y);
            tempRect.setHeight(rectStartCoordinates.y - tempRect.getY());
        }
    }

    public void onMouseReleased(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() != MouseButton.PRIMARY) {
            return;
        }
        rectStartCoordinates.x = roundToImageHorizontalBounds(rectStartCoordinates.x);
        rectStartCoordinates.y = roundToImageVerticalBounds(rectStartCoordinates.y);
        double rectEndX = roundToImageHorizontalBounds(mouseEvent.getX());
        double rectEndY = roundToImageVerticalBounds(mouseEvent.getY());

        if (rectStartCoordinates.x == rectEndX) {
            isDragSelectionActive = false;
            removeTempRect();
            updateView();
            return;
        }
        int x1 = getImagePixelX(rectStartCoordinates.x);
        int x2 = getImagePixelX(rectEndX);
        int y1 = getImagePixelY(rectStartCoordinates.y);
        int y2 = getImagePixelY(rectEndY);

        Component c;
        if (strapCheckBox.isSelected())
            c = dropWhiteSpace(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2));
        else
            c = new Component(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2),
                    classSpinner.getValue(), imgProperties.getWidth(), imgProperties.getHeight(),
                    imgProperties.getName());

        if (c.getMaxX() > c.getMinX() + 5) {
            components.get(currentImageIndex).add(c);
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            isDragSelectionActive = false;
                        }
                    },
                    100
            );
        }
        removeTempRect();
        updateView();
    }

    private void removeTempRect() {
        tempRect.setX(0);
        tempRect.setY(0);
        tempRect.setWidth(0);
        tempRect.setHeight(0);

        parent.getChildren().remove(tempRect);
    }

    private void removeAllRects() {
        parent.getChildren().removeIf(p -> p instanceof Rectangle);
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
        double aspectRatio = imgProperties.getAspectRatio();
        return Math.min(imageView.getFitHeight(), imageView.getFitWidth() / aspectRatio);
    }

    private double getShownImageWidth() {
        double aspectRatio = imgProperties.getAspectRatio();
        return Math.min(imageView.getFitWidth(), imageView.getFitHeight() * aspectRatio);
    }

    private double roundToImageHorizontalBounds(double x) {
        if (x < 0)
            return 0;
        double shownImageWidth = getShownImageWidth();
        if (x > shownImageWidth)
            return shownImageWidth;
        return x;
    }


    private void drawComponent(Component c) {
        Rectangle finalRect = new Rectangle(getImageViewX(c.getMinX()), getImageViewY(c.getMinY()),
                getImageViewX(c.getMaxX() - c.getMinX()), getImageViewY(c.getMaxY() - c.getMinY()));

        finalRect.setStroke(RECT_COLORS[(c.getClassId() - 1) % 6]);
        finalRect.setFill(RECT_COLORS[(c.getClassId() - 1) % 6].deriveColor(0, 1.2, 1, 0.6));
        finalRect.setStrokeWidth(1);
        finalRect.setStrokeLineCap(StrokeLineCap.ROUND);
        finalRect.setMouseTransparent(true);
        parent.getChildren().add(finalRect);
    }

    private double getImageViewX(int imagePixelX) {
        return ((double) imagePixelX) * getShownImageWidth() / imgProperties.getWidth();
    }

    private double getImageViewY(int imagePixelY) {
        return ((double) imagePixelY) * getShownImageHeight() / imgProperties.getHeight();
    }

    private int getImagePixelX(double imageViewX) {
        return (int) Math.round(imageViewX * (imgProperties.getWidth() / getShownImageWidth()));
    }

    private int getImagePixelY(double imageViewY) {
        return (int) Math.round(imageViewY * (imgProperties.getHeight() / getShownImageHeight()));
    }

    public void saveComponents() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Script");
        File scriptFile = fileChooser.showSaveDialog(parent.getScene().getWindow());
        writeToFile(scriptFile, getMaskScript());
        File jsonFile = new File(scriptFile.getPath() + ".json");
        writeToFile(jsonFile, getComponentsJson().toString());
    }

    private void writeToFile(File file, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONObject getComponentsJson() {
        JSONObject obj = new JSONObject();
        for (int i = 0; i < images.size(); i++) {
            Image image = new Image(images.get(i).toURI().toString());
            String paper = images.get(i).getName().split("-")[0];
            try {
                int page = Integer.parseInt(images.get(i).getName().split("-")[images.get(i).getName()
                        .split("-").length - 1].split("\\.")[0]) + 1;

                if (!obj.keySet().contains(paper)) {
                    JSONObject paperObj = new JSONObject();
                    paperObj.put("components", new HashSet<>());
                    paperObj.put("pages_annotated", new HashSet<>());
                    obj.put(paper, paperObj);
                }
                obj.getJSONObject(paper).getJSONArray("pages_annotated").put(page);

                double paperWidth = image.getWidth();
                double paperHeight = image.getHeight();

                for (int j = 0; j < components.get(i).size(); j++) {
                    Component f = components.get(i).get(j);
                    if (f.getMaxY() < 0 || f.getMaxY() > image.getHeight())
                        continue;

                    JSONObject component = new JSONObject();

                    component.put("class_id", f.getClassId());
                    JSONArray region = new JSONArray();
                    region.put(f.getMinX());
                    region.put(f.getMinY());
                    region.put(f.getMaxX());
                    region.put(f.getMaxY());
                    component.put("region", region);
                    component.put("page", page);
                    component.put("page_width", paperWidth);
                    component.put("page_height", paperHeight);

                    obj.getJSONObject(paper).getJSONArray("components").put(component);

                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return obj;
    }

    private String getMaskScript() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < images.size(); i++) {
            Image image = new Image(images.get(i).toURI().toString());
            sb.append("convert -size ").append(image.getWidth()).append("x").append(image.getHeight())
                    .append(" xc:black ");

            for (int j = 0; j < components.get(i).size(); j++) {
                Component f = components.get(i).get(j);
                if (f.getMaxY() < 0 || f.getMaxY() > image.getHeight())
                    continue;
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

    // Blue represents class, red and green are generated for humans
    private String getFragmentColor(Component component) {
        int red = (200 - 100 * component.getClassId()) % 256;
        int green = 100 * (-1 + component.getClassId()) % 256;
        return "\'rgb(" + red + "," + green + "," + component.getClassId() + ")\'";
    }

    private String getOutputFileName(int fileIndex) {
        return removeFileExtension(images.get(fileIndex).getName()) + ".png";
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
        components.clear();
        deletedComponents.clear();
        removeAllRects();

        if (images != null && images.size() > 0) {
            currentImageIndex = 0;
            showImage(images.get(currentImageIndex));

            for (int i = 0; i < images.size(); i++) {
                components.add(new ArrayList<>());
                deletedComponents.add(new Stack<>());
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
        if (images.size() > 0 && images.size() > currentImageIndex) {
            showImage(images.get(currentImageIndex));

            updateView();
        }
    }

    public void undoSelection() {
        if (!components.isEmpty() && !deletedComponents.isEmpty()) {
            deletedComponents.get(currentImageIndex).push(components.get(currentImageIndex).get(components.get(currentImageIndex).size() - 1));
            components.get(currentImageIndex).remove(components.get(currentImageIndex).size() - 1);
            prevComponentCells = new ConnectedCells(gridWidthSpinner.getValue());
            updateView();
        }
    }

    public void redoSelection() {
        if (!deletedComponents.get(currentImageIndex).isEmpty()) {
            components.get(currentImageIndex).add(deletedComponents.get(currentImageIndex).pop());
            updateView();
        }
    }

    private void updateView() {
        removeAllRects();
        for (Component c : components.get(currentImageIndex)) {
            drawComponent(c);
        }
    }

    public void onClick(MouseEvent mouseEvent) {
        if (isDragSelectionActive) {
            return;
        }
        if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            undoSelection();
        }
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            smartSelect(mouseEvent);
        }
    }

    private void smartSelect(MouseEvent mouseEvent) {
        Point imagePixelCoordinates = getImagePixelCoordinate(mouseEvent.getX(), mouseEvent.getY());
        int gridWidth = gridWidthSpinner.getValue();
        int gridIndexX = (int) ((double) imagePixelCoordinates.x / gridWidth);
        int gridIndexY = (int) ((double) imagePixelCoordinates.y / gridWidth);
        if (!hasColoredPixel(new Cell(gridIndexX, gridIndexY), gridWidth))
            return;

        ConnectedCells initialCells = new ConnectedCells(gridWidth);
        if (mouseEvent.isControlDown()) {
            gridWidth = prevGridWidth;
            gridWidthSpinner.getValueFactory().setValue(prevGridWidth);
            initialCells = prevComponentCells;
            if (!prevComponentCells.getCells().isEmpty())
                undoSelection();
        }
        ConnectedCells connectedCells = getConnectedCells(gridIndexX, gridIndexY, gridWidth, initialCells);
        connectedCells = extendComponent(connectedCells, gridWidth);
        prevComponentCells = connectedCells;
        prevGridWidth = gridWidth;
        Component component = dropWhiteSpace(connectedCells.getMinX(), connectedCells.getMinY(),
                connectedCells.getMaxX(), connectedCells.getMaxY());
        components.get(currentImageIndex).add(component);
        updateView();
    }

    private Point getImagePixelCoordinate(double imageViewX, double imageViewY) {
        int imagePixelX = getImagePixelX(imageViewX);
        int imagePixelY = getImagePixelY(imageViewY);
        return new Point(imagePixelX, imagePixelY);
    }

    private Component dropWhiteSpace(int minX, int minY, int maxX, int maxY) {

        for (int i = minX; i < maxX; i++) {
            boolean onlyWhite = true;
            for (int j = minY; j < maxY; j++) {
                if (!isImgWhite[j][i]) {
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
                if (!isImgWhite[j][i]) {
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
                if (!isImgWhite[j][i]) {
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
                if (!isImgWhite[j][i]) {
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
        return new Component(minX, minY, maxX, maxY, (int) classSpinner.getValue(), imgProperties.getWidth(),
                imgProperties.getHeight(), imgProperties.getName());
    }

    private ConnectedCells extendComponent(ConnectedCells component, int gridWidth) {
        // Make component rectangular
        int minX = component.getMinCellIndexX();
        int maxX = component.getMaxCellIndexX();
        int minY = component.getMinCellIndexY();
        int maxY = component.getMaxCellIndexY();
        boolean outOfRange = false;

        for (int i = minX; i < maxX + 1; i++) {
            for (int j = minY; j < maxY + 1; j++) {
                if (!component.contains(i, j)) {
                    for (Cell c : getConnectedCells(i, j, gridWidth, component).getCells()) {
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
            component = extendComponent(component, gridWidth);
        }
        return component;
    }

    private ConnectedCells getConnectedCells(int gridIndexX, int gridIndexY, int gridWidth, ConnectedCells cells) {
        Cell c = new Cell(gridIndexX, gridIndexY);
        if (hasColoredPixel(c, gridWidth)) {
            cells.insertCell(c);
            if (!cells.contains(gridIndexX, gridIndexY - 1))
                cells = getConnectedCells(gridIndexX, gridIndexY - 1, gridWidth, cells);
            if (!cells.contains(gridIndexX + 1, gridIndexY))
                cells = getConnectedCells(gridIndexX + 1, gridIndexY, gridWidth, cells);
            if (!cells.contains(gridIndexX, gridIndexY + 1))
                cells = getConnectedCells(gridIndexX, gridIndexY + 1, gridWidth, cells);
            if (!cells.contains(gridIndexX - 1, gridIndexY))
                cells = getConnectedCells(gridIndexX - 1, gridIndexY, gridWidth, cells);
        }
        return cells;
    }

    private boolean hasColoredPixel(Cell c, int gridWidth) {
        for (int i = c.getIndexX() * gridWidth; i < (c.getIndexX() + 1) * gridWidth && i >= 0 && i < imgProperties.getWidth(); i++) {
            for (int j = c.getIndexY() * gridWidth; j < (c.getIndexY() + 1) * gridWidth && j >= 0 && j < imgProperties.getHeight(); j++) {
                if (!isImgWhite[j][i]) {
                    return true;
                }
            }
        }
        return false;
    }

    // Returns array of bools containing information if the pixel is white
    private boolean[][] convertToBoolArray(BufferedImage image) {

        final int width = image.getWidth();
        final int height = image.getHeight();
        boolean[][] result = new boolean[height][width];
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++) {
                result[j][i] = image.getRGB(i, j) == -1;
            }
        return result;
    }

    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.RIGHT) {
            nextImage();
        }
        if (keyEvent.getCode() == KeyCode.LEFT) {
            prevImage();
        }
        if (keyEvent.getCode() == KeyCode.UP) {
            classSpinner.increment();
        }
        if (keyEvent.getCode() == KeyCode.DOWN) {
            classSpinner.decrement();
        }
    }

    public void onMouseEntered(MouseEvent mouseEvent) {
        if (linesCheckBox.isSelected()) {
            if (!parent.getChildren().contains(lineX))
                parent.getChildren().add(lineX);
            if (!parent.getChildren().contains(lineY))
                parent.getChildren().add(lineY);

            lineX.setVisible(true);
            lineY.setVisible(true);
        }
    }

    public void onMouseMoved(MouseEvent mouseEvent) {
        if (linesCheckBox.isSelected()) {
            double x = (mouseEvent.getX());
            double y = (mouseEvent.getY());

            if (lineX.getStartY() != y) {
                lineX.setStartX(0);
                lineX.setEndX(getShownImageWidth());

                lineX.setStartY(y);
                lineX.setEndY(y);
            }

            if (lineY.getStartX() != x) {
                lineY.setStartY(0);
                lineY.setEndY(getShownImageHeight());

                lineY.setStartX(x);
                lineY.setEndX(x);
            }
        }
    }

    public void onMouseExited(MouseEvent mouseEvent) {
        lineX.setVisible(false);
        lineY.setVisible(false);
    }

    public void mouseScroll(ScrollEvent scrollEvent) {
        if (scrollEvent.getDeltaY() > 0) {
            classSpinner.increment();
        } else {
            classSpinner.decrement();
        }
    }

}
