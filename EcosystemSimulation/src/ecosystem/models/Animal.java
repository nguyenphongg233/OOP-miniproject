package ecosystem.models;

import java.util.List;
import java.util.Random;

public abstract class Animal extends Organism {
    protected int moveCost;
    private static final Random RNG = new Random();

    public Animal(int x, int y, int energy, int moveCost) {
        super(x, y, energy);
        this.moveCost = moveCost;
    }

    protected void moveTowards(int targetX, int targetY, Grid grid) {
        int dx = 0, dy = 0;
        if (targetX > this.x) dx = 1;
        else if (targetX < this.x) dx = -1;
        if (targetY > this.y) dy = 1;
        else if (targetY < this.y) dy = -1;
        int nx = Math.max(0, Math.min(grid.getWidth() - 1, this.x + dx));
        int ny = Math.max(0, Math.min(grid.getHeight() - 1, this.y + dy));
        if (grid.isCellEmpty(nx, ny)) {
            this.x = nx; this.y = ny;
            this.energy -= moveCost;
        }
    }

    protected void randomMove(Grid grid) {
        List<int[]> neighbors = grid.getNeighborPositions(x, y);
        if (!neighbors.isEmpty()) {
            int[] pos = neighbors.get(RNG.nextInt(neighbors.size()));
            if (grid.isCellEmpty(pos[0], pos[1])) {
                this.x = pos[0]; this.y = pos[1];
                this.energy -= moveCost;
            }
        }
    }
}
