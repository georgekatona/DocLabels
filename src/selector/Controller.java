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
    private ImageView imageToLabel;

    private int currentImageIndex;
    private List<File> images;
    private List<List<Fragment>> fragments = new ArrayList<>();

    private List<Stack<Fragment>> deletedFragments = new Stack<>();

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

    public void saveFragments() {
        if (!isFragmentsEmpty()) {
            saveLabelRatio();
        }
    }

    private boolean isFragmentsEmpty() {
        for (List<Fragment> list : fragments) {
            if (!list.isEmpty())
                return false;
        }
        return true;
    }

    private void saveLabelRatio() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Ratio");
            File ratioFile = fileChooser.showSaveDialog(parent.getScene().getWindow());
            BufferedWriter writer = new BufferedWriter(new FileWriter(ratioFile));
            writer.write(getLabelRatioData());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getLabelRatioData() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < images.size(); i++) {
            List<Fragment> fragments = this.fragments.get(i);
            if (!fragments.isEmpty()) {
                sb.append(images.get(i).getName()).append(",")
                        .append(calculateLabelRatio(fragments, Label.TEXT)).append(",")
                        .append(calculateLabelRatio(fragments, Label.FORMULA)).append(",")
                        .append(calculateLabelRatio(fragments, Label.TABLE)).append(",")
                        .append(calculateLabelRatio(fragments, Label.REFERENCE)).append(",")
                        .append(calculateLabelRatio(fragments, Label.FIGURE)).append("\n");
            }
        }
        return sb.toString();
    }

    private double calculateLabelRatio(List<Fragment> fragments, Label label) {
        double sumArea = 0;
        double labelArea = 0;
        for (Fragment f : fragments) {
            double fragmentArea = (f.getWidth() * f.getHeight());
            sumArea += fragmentArea;
            if (f.getLabel().equals(label)) {
                labelArea += fragmentArea;
            }
        }
        return labelArea / sumArea;
    }

    public void openImages() {
        FileChooser fileChooser = new FileChooser();
        images = fileChooser.showOpenMultipleDialog(parent.getScene().getWindow());
        fragments.clear();
        deletedFragments.clear();
        removeAllRects();

        if (images != null && images.size() > 0) {
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
