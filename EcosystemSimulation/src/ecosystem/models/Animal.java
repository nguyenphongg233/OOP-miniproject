package ecosystem.models;

import java.util.List;
import java.util.Random;

public abstract class Animal extends Organism {
    private int moveCost;
    private static final Random RNG = new Random();

    public Animal(int x, int y, int energy, int moveCost) {
        super(x, y, energy);
        this.moveCost = moveCost;
    }

    protected int getMoveCost() { return moveCost; }

    protected void moveTowards(int targetX, int targetY, Grid grid) {
        int dx = 0, dy = 0;
        if (targetX > this.getX()) dx = 1;
        else if (targetX < this.getX()) dx = -1;
        if (targetY > this.getY()) dy = 1;
        else if (targetY < this.getY()) dy = -1;
        int nx = Math.max(0, Math.min(grid.getWidth() - 1, this.getX() + dx));
        int ny = Math.max(0, Math.min(grid.getHeight() - 1, this.getY() + dy));
        if (grid.isCellEmpty(nx, ny)) {
            this.setPosition(nx, ny);
            this.adjustEnergy(-getMoveCost());
        }
    }

    protected void randomMove(Grid grid) {
        List<int[]> neighbors = grid.getNeighborPositions(getX(), getY());
        if (!neighbors.isEmpty()) {
            int[] pos = neighbors.get(RNG.nextInt(neighbors.size()));
            if (grid.isCellEmpty(pos[0], pos[1])) {
                this.setPosition(pos[0], pos[1]);
                this.adjustEnergy(-getMoveCost());
            }
        }
    }
}
