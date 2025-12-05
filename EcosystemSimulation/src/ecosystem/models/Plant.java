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
        age++;
        if (RNG.nextDouble() < grid.getPlantGrowRate()) {
            List<int[]> empties = grid.getEmptyNeighbors(x, y);
            if (!empties.isEmpty()) {
                int[] pos = empties.get(RNG.nextInt(empties.size()));
                grid.addOrganism(new Plant(pos[0], pos[1], grid.getPlantEnergy()));
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
