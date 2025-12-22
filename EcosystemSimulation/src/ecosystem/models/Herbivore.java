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
        incrementAge();
        Organism plant = grid.findNeighborOfType(getX(), getY(), Plant.class);
        if (plant != null) {
            grid.removeOrganism(plant);
            this.adjustEnergy(eatGain);
        } else {
            this.randomMove(grid);
        }

        if (this.getEnergy() >= grid.getHerbivoreReproduceThreshold()) {
            int half = this.getEnergy() / 2;
            this.setEnergy(half);
            List<int[]> empties = grid.getEmptyNeighbors(getX(), getY());
            if (!empties.isEmpty()) {
                int[] pos = empties.get(RNG.nextInt(empties.size()));
                grid.addOrganism(new Herbivore(pos[0], pos[1], this.getEnergy(), this.getMoveCost(), this.eatGain));
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
