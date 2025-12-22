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
        incrementAge();
        Organism prey = grid.findNeighborOfType(getX(), getY(), Herbivore.class);
        if (prey != null) {
            moveTowards(prey.getX(), prey.getY(), grid);
            if (this.getX() == prey.getX() && this.getY() == prey.getY()) {
                grid.removeOrganism(prey);
                this.adjustEnergy(eatGain);
            }
        } else {
            this.randomMove(grid);
        }

        if (this.getEnergy() >= grid.getCarnivoreReproduceThreshold()) {
            int half = this.getEnergy() / 2;
            this.setEnergy(half);
            List<int[]> empties = grid.getEmptyNeighbors(getX(), getY());
            if (!empties.isEmpty()) {
                int[] pos = empties.get(RNG.nextInt(empties.size()));
                grid.addOrganism(new Carnivore(pos[0], pos[1], this.getEnergy(), this.getMoveCost(), this.eatGain));
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
