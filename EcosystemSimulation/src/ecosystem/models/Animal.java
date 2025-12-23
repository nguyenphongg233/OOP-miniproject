package ecosystem.models;

import java.util.List;
import java.util.Random;

public abstract class Animal extends Organism {
    private int moveCost;
    // Per-individual energy parameters
    private int reproduceThreshold;
    private int metabolismCost;
    private double absorptionRate;
    private static final Random RNG = new Random();

    public Animal(int x, int y, int energy, int moveCost,
                  int reproduceThreshold, int metabolismCost, double absorptionRate) {
        super(x, y, energy);
        this.moveCost = moveCost;
        this.reproduceThreshold = reproduceThreshold;
        this.metabolismCost = metabolismCost;
        this.absorptionRate = absorptionRate;
    }

    protected int getMoveCost() { return moveCost; }
    protected int getReproduceThreshold() { return reproduceThreshold; }
    protected int getMetabolismCost() { return metabolismCost; }
    protected double getAbsorptionRate() { return absorptionRate; }

    @Override
    public void onDayStart() {
        // Increase age and apply basal metabolism cost
        super.onDayStart();
        if (metabolismCost != 0) {
            adjustEnergy(-metabolismCost);
        }
    }

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

    /**
     * Choose a random empty neighboring cell as a movement target,
     * or return null to indicate no available move.
     */
    protected int[] chooseRandomMoveTarget(Grid grid) {
        List<int[]> neighbors = grid.getNeighborPositions(getX(), getY());
        if (neighbors.isEmpty()) return null;
        java.util.Collections.shuffle(neighbors, RNG);
        for (int[] pos : neighbors) {
            // Only move onto empty, walkable terrain (avoid water, etc.)
            if (grid.isCellEmpty(pos[0], pos[1]) && grid.isWalkableTerrain(pos[0], pos[1])) {
                return pos;
            }
        }
        return null;
    }
}
