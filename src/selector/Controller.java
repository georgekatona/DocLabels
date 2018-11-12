package selector;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Controller {

    private static final String LABEL_DATA_NAME = "label_data";

    private final Rectangle rect;

    private double xStart;
    private double xEnd;
    private double yStart;
    private double yEnd;

    private double imageWidth;
    private double imageHeight;

    private double ivWidth;
    private double ivHeight;

    @FXML
    private AnchorPane parent;
    @FXML
    private CheckBox saveLabelsDataCB;
    @FXML
    private ImageView imageToLabel;

    private int currentImageIndex;
    private List<File> images;
    private List<List<Fragment>> fragments = new ArrayList<>();

    List<Stack<Fragment>> deletedFragments = new Stack<>();
    private File scriptFile;

    public Controller() {
        rect = new Rectangle(0, 0, 0, 0);
        rect.setStroke(Color.BLUE);
        rect.setStrokeWidth(1);
        rect.setStrokeLineCap(StrokeLineCap.ROUND);
        rect.setFill(Color.LIGHTBLUE.deriveColor(0, 1.2, 1, 0.6));
    }

    private void showImage(File file) {
        Image image = new Image(file.toURI().toString());
        imageWidth = image.getWidth();
        imageHeight = image.getHeight();
        imageToLabel.setImage(image);

        ivWidth = imageToLabel.getFitWidth();
        ivHeight = imageToLabel.getFitHeight();
    }

    public void startSelection(MouseEvent mouseEvent) {
        xStart = mouseEvent.getX();
        yStart = mouseEvent.getY();

        rect.setX(xStart);
        rect.setY(yStart);
        rect.setWidth(0);
        rect.setHeight(0);

        parent.getChildren().add(rect);
    }

    public void onMouseDragged(MouseEvent mouseEvent) {
        double x = roundToImageHorizontalBounds(mouseEvent.getX());
        double y = roundToImageVerticalBounds(mouseEvent.getY());
        double offsetX = x - xStart;
        double offsetY = y - yStart;

        if (offsetX > 0)
            rect.setWidth(offsetX);
        else {
            rect.setX(x);
            rect.setWidth(xStart - rect.getX());
        }
        if (offsetY > 0) {
            rect.setHeight(offsetY);
        } else {
            rect.setY(y);
            rect.setHeight(yStart - rect.getY());
        }
    }

    public void endSelection(MouseEvent mouseEvent) {
        xEnd = mouseEvent.getX();
        yEnd = mouseEvent.getY();

        xStart = roundToImageHorizontalBounds(xStart);
        xEnd = roundToImageHorizontalBounds(xEnd);

        yStart = roundToImageVerticalBounds(yStart);
        yEnd = roundToImageVerticalBounds(yEnd);

        showPopupMenu();

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
        for (Label l : Label.values()) {
            MenuItem item = new MenuItem(l.name());
            item.setOnAction(event -> selectLabel(l));
            items.add(item);
        }

        contextMenu.getItems().addAll(items);
        contextMenu.show(parent, xEnd, yEnd);
    }

    private void selectLabel(Label label) {
        Fragment f = new Fragment(Math.min(xStart, xEnd), Math.min(yStart, yEnd),
                Math.abs(xStart - xEnd), Math.abs(yStart - yEnd), label);
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
        Rectangle finalRect = new Rectangle(f.getxOffset(), f.getyOffset(), f.getWidth(), f.getHeight());
        switch (f.getLabel()) {
            case TEXT:
                finalRect.setStroke(Color.BLACK);
                finalRect.setFill(Color.LIGHTGRAY.deriveColor(0, 1.2, 1, 0.6));
                break;
            case FORMULA:
                finalRect.setStroke(Color.YELLOW);
                finalRect.setFill(Color.YELLOW.deriveColor(0, 1.2, 1, 0.6));
                break;
            case TABLE:
                finalRect.setStroke(Color.PINK);
                finalRect.setFill(Color.LIGHTPINK.deriveColor(0, 1.2, 1, 0.6));
                break;
            case REFERENCE:
                finalRect.setStroke(Color.DARKCYAN);
                finalRect.setFill(Color.CYAN.deriveColor(0, 1.2, 1, 0.6));
                break;
            case FIGURE:
                finalRect.setStroke(Color.GREEN);
                finalRect.setFill(Color.LIGHTGREEN.deriveColor(0, 1.2, 1, 0.6));
                break;
        }

        finalRect.setStrokeWidth(1);
        finalRect.setStrokeLineCap(StrokeLineCap.ROUND);
        parent.getChildren().add(finalRect);
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
            if (saveLabelsDataCB.isSelected()) {
                saveLabels();
            }
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
            scriptFile = fileChooser.showSaveDialog(parent.getScene().getWindow());
            BufferedWriter writer = new BufferedWriter(new FileWriter(scriptFile));
            writer.write(getMaskScript());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveLabels() {
        try {
            File file = new File(scriptFile.getParent() + File.separator + LABEL_DATA_NAME);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(getLabelData());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getLabelData() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < images.size(); i++)
            for (int j = 0; j < fragments.get(i).size(); j++) {
                sb.append(getOutputFileName(i)).append(";").append(fragments.get(i).get(j).getLabel()).append("\n");
            }
        return sb.toString();
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
                        .append(getWidthPixelsFromCoordinate(f.getxOffset()))
                        .append(",")
                        .append(getHeightPixelsFromCoordinate(f.getyOffset()))
                        .append(" ")
                        .append(getWidthPixelsFromCoordinate(f.getxOffset() + f.getWidth()))
                        .append(",")
                        .append(getHeightPixelsFromCoordinate(f.getyOffset() + f.getHeight()))
                        .append("\" ");
            }
            sb.append(getOutputFileName(i)).append("\n");
        }
        return sb.toString();
    }

    private String getFragmentColor(Fragment fragment) {
        switch (fragment.getLabel()) {
            case TEXT:
                return "green";
            case FORMULA:
                return "purple";
            case TABLE:
                return "yellow";
            case REFERENCE:
                return "blue";
            case FIGURE:
                return "red";
            default:
                return "white";
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

}
