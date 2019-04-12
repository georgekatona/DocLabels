package selector;

import java.util.HashSet;

public class ConnectedComponent {

    private int cellWidth;

    private HashSet<Cell> cells = new HashSet<>();

    public ConnectedComponent(int cellWidth) {
        this.cellWidth = cellWidth;
    }

    public void insertCell(Cell newCell) {
        cells.add(newCell);
    }

    public HashSet<Cell> getCells(){
        return cells;
    }

    public int getMinCellIndexX() {
        int min = Integer.MAX_VALUE;
        for (Cell c : cells) {
            if (c.getIndexX() < min)
                min = c.getIndexX();
        }
        return min;
    }

    public int getMaxCellIndexX() {
        int max = Integer.MIN_VALUE;
        for (Cell c : cells) {
            if (c.getIndexX() > max)
                max = c.getIndexX();
        }
        return max;
    }


    public int getMinCellIndexY() {
        int min = Integer.MAX_VALUE;
        for (Cell c : cells) {
            if (c.getIndexY() < min)
                min = c.getIndexY();
        }
        return min;
    }

    public int getMaxCellIndexY() {
        int max = Integer.MIN_VALUE;
        for (Cell c : cells) {
            if (c.getIndexY() > max)
                max = c.getIndexY();
        }
        return max;
    }

    public int getMinX() {
        return getMinCellIndexX() * cellWidth;
    }

    public int getMaxX() {
        return ((getMaxCellIndexX() + 1) * cellWidth - 1);
    }

    public int getMinY() {
        return getMinCellIndexY() * cellWidth;
    }

    public int getMaxY() {
        return ((getMaxCellIndexY() + 1) * cellWidth - 1);
    }

    public boolean contains(int indexX, int indexY) {
        return cells.contains(new Cell(indexX, indexY));
    }
}
