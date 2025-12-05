package ecosystem.models;

import java.util.List;
import java.util.Random;

public class Herbivore extends Animal {
    private int eatGain;
    private static final Random RNG = new Random();

    public Herbivore(int x, int y, int energy, int moveCost, int eatGain) {
        super(x, y, energy, moveCost);
        this.eatGain = eatGain;
    }

    @Override
    public void step(Grid grid) {
        age++;
        Organism plant = grid.findNeighborOfType(x, y, Plant.class);
        if (plant != null) {
            grid.removeOrganism(plant);
            this.energy += eatGain;
        } else {
            this.randomMove(grid);
        }

        if (this.energy >= grid.getHerbivoreReproduceThreshold()) {
            this.energy = this.energy / 2;
            List<int[]> empties = grid.getEmptyNeighbors(x, y);
            if (!empties.isEmpty()) {
                int[] pos = empties.get(RNG.nextInt(empties.size()));
                grid.addOrganism(new Herbivore(pos[0], pos[1], this.energy, this.moveCost, this.eatGain));
            }
        }
    }

    @Override
    public String toString() {
        return "h";
    }

    @Override
    public String getType() { return "Herbivore"; }
}
