package ecosystem.models;

import java.util.List;
import java.util.Random;

public class Carnivore extends Animal {
    private int eatGain;
    private static final Random RNG = new Random();

    public Carnivore(int x, int y, int energy, int moveCost, int eatGain,
                     int reproduceThreshold, int metabolismCost, double absorptionRate) {
        super(x, y, energy, moveCost, reproduceThreshold, metabolismCost, absorptionRate);
        this.eatGain = eatGain;
    }

    @Override
    public void step(Grid grid) {
        // Fallback single-organism update (not used in new multi-phase engine):
        eat(grid);
        tryReproduce(grid);
    }

    /** Eating phase: look for a neighboring herbivore and consume it. */
    public void eat(Grid grid) {
        Organism prey = grid.findNeighborOfType(getX(), getY(), Herbivore.class);
        if (prey != null) {
            grid.removeOrganism(prey);
            int gained = (int)Math.round(eatGain * getAbsorptionRate());
            if (gained <= 0 && eatGain > 0) gained = 1;
            this.adjustEnergy(gained);
        }
    }

    /** Reproduction phase using individual energy threshold and energy splitting. */
    public void tryReproduce(Grid grid) {
        if (this.getEnergy() >= getReproduceThreshold()) {
            List<int[]> empties = grid.getEmptyNeighbors(getX(), getY());
            java.util.List<int[]> walkable = new java.util.ArrayList<>();
            for (int[] p : empties) {
                if (grid.isWalkableTerrain(p[0], p[1])) walkable.add(p);
            }
            if (!walkable.isEmpty()) {
                int originalEnergy = this.getEnergy();
                int parentAfter = originalEnergy / 2;
                int childEnergy = originalEnergy / 4;
                if (childEnergy <= 0) return;
                this.setEnergy(parentAfter);
                int[] pos = walkable.get(RNG.nextInt(walkable.size()));
                Carnivore child = new Carnivore(
                    pos[0], pos[1],
                    childEnergy,
                    getMoveCost(),
                    this.eatGain,
                    getReproduceThreshold(),
                    getMetabolismCost(),
                    getAbsorptionRate()
                );
                grid.addOrganism(child);
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
