package ecosystem.models;

import java.util.List;
import java.util.Random;

public class Plant extends Organism {
    private static final Random RNG = new Random();

    public Plant(int x, int y, int energy) {
        super(x, y, energy);
    }

    @Override
    public void step(Grid grid) {
        // Fallback to per-day growth behavior (used if stepAll() is called directly).
        dayUpdate(grid);
    }

    /**
     * Per-day plant growth: from each plant cell, attempt to spread
     * independently to the 4 orthogonal neighboring cells.
     * A new plant only grows into an empty cell on suitable terrain (grass).
     */
    public void dayUpdate(Grid grid) {
        int x = getX();
        int y = getY();
        int[][] dirs = new int[][] { {1,0}, {-1,0}, {0,1}, {0,-1} };
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (!grid.inBounds(nx, ny)) continue;
            if (!grid.isCellEmpty(nx, ny)) continue; // occupied by any organism -> no growth
            if (!grid.isPlantHabitable(nx, ny)) continue; // chỉ mọc trên vùng cỏ (grass)
            if (RNG.nextDouble() < grid.getPlantGrowRate()) {
                grid.addOrganism(new Plant(nx, ny, grid.getPlantEnergy()));
            }
        }
    }

    @Override
    public String toString() {
        return "P";
    }

    @Override
    public String getType() { return "Plant"; }
}
