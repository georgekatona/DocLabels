package selector;

import java.util.HashSet;

class ConnectedCells {

    private int cellWidth;

    private HashSet<Cell> cells = new HashSet<>();

    ConnectedCells(int cellWidth) {
        this.cellWidth = cellWidth;
    }

    void insertCell(Cell newCell) {
        cells.add(newCell);
    }

    HashSet<Cell> getCells(){
        return cells;
    }

    int getMinCellIndexX() {
        int min = Integer.MAX_VALUE;
        for (Cell c : cells) {
            if (c.getIndexX() < min)
                min = c.getIndexX();
        }
        return min;
    }

    int getMaxCellIndexX() {
        int max = Integer.MIN_VALUE;
        for (Cell c : cells) {
            if (c.getIndexX() > max)
                max = c.getIndexX();
        }
        return max;
    }


    int getMinCellIndexY() {
        int min = Integer.MAX_VALUE;
        for (Cell c : cells) {
            if (c.getIndexY() < min)
                min = c.getIndexY();
        }
        return min;
    }

    int getMaxCellIndexY() {
        int max = Integer.MIN_VALUE;
        for (Cell c : cells) {
            if (c.getIndexY() > max)
                max = c.getIndexY();
        }
        return max;
    }

    int getMinX() {
        return getMinCellIndexX() * cellWidth;
    }

    int getMaxX() {
        return ((getMaxCellIndexX() + 1) * cellWidth - 1);
    }

    int getMinY() {
        return getMinCellIndexY() * cellWidth;
    }

    int getMaxY() {
        return ((getMaxCellIndexY() + 1) * cellWidth - 1);
    }

    boolean contains(int indexX, int indexY) {
        return cells.contains(new Cell(indexX, indexY));
    }
}
