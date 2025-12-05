package ecosystem.models;

import java.util.List;
import java.util.Random;

public class Carnivore extends Animal {
    private int eatGain;
    private static final Random RNG = new Random();

    public Carnivore(int x, int y, int energy, int moveCost, int eatGain) {
        super(x, y, energy, moveCost);
        this.eatGain = eatGain;
    }

    @Override
    public void step(Grid grid) {
        age++;
        Organism prey = grid.findNeighborOfType(x, y, Herbivore.class);
        if (prey != null) {
            moveTowards(prey.getX(), prey.getY(), grid);
            if (this.x == prey.getX() && this.y == prey.getY()) {
                grid.removeOrganism(prey);
                this.energy += eatGain;
            }
        } else {
            this.randomMove(grid);
        }

        if (this.energy >= grid.getCarnivoreReproduceThreshold()) {
            this.energy = this.energy / 2;
            List<int[]> empties = grid.getEmptyNeighbors(x, y);
            if (!empties.isEmpty()) {
                int[] pos = empties.get(RNG.nextInt(empties.size()));
                grid.addOrganism(new Carnivore(pos[0], pos[1], this.energy, this.moveCost, this.eatGain));
            }
        }
    }

    @Override
    public String toString() {
        return "C";
    }

    @Override
    public String getType() { return "Carnivore"; }
}
