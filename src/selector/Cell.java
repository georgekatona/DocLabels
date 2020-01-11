package selector;

public class Cell {
    private int indexX;
    private int indexY;

    public Cell(int indexX, int indexY) {
        this.indexX = indexX;
        this.indexY = indexY;
    }

    public int getIndexX() {
        return indexX;
    }

    public int getIndexY() {
        return indexY;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Cell))
            return false;
        if (obj == this)
            return true;
        return this.indexX == ((Cell) obj).indexX && this.indexY == ((Cell) obj).indexY;
    }

    public int hashCode(){
        return indexX * indexY;
    }
}
