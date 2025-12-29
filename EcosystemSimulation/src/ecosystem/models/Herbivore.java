/**
 * Herbivore organism: moves, eats plants, reproduces under energy threshold.
 */
package ecosystem.models;

import java.util.List;
import java.util.Random;

public class Herbivore extends Animal {
    private int eatGain;
    private static final Random RNG = new Random();

    public Herbivore(int x, int y, int energy, int moveCost, int eatGain,
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

    /** Eating phase: look for a neighboring plant and consume it. */
    public void eat(Grid grid) {
        Organism plant = grid.findNeighborOfType(getX(), getY(), Plant.class);
        if (plant != null) {
            grid.removeOrganism(plant);
            int gained = (int)Math.round(eatGain * getAbsorptionRate());
            if (gained <= 0 && eatGain > 0) gained = 1;
            this.adjustEnergy(gained);
        }
    }

    /** Reproduction phase using individual energy threshold and energy splitting. */
    public void tryReproduce(Grid grid) {
        if (this.getEnergy() >= getReproduceThreshold()) {
            List<int[]> empties = grid.getEmptyNeighbors(getX(), getY());
            // chỉ sinh con vào các ô trống có địa hình đi được (không sinh ra giữa biển)
            java.util.List<int[]> walkable = new java.util.ArrayList<>();
            for (int[] p : empties) {
                if (grid.isWalkableTerrain(p[0], p[1])) walkable.add(p);
            }
            if (!walkable.isEmpty()) {
                int originalEnergy = this.getEnergy();
                int parentAfter = originalEnergy / 2;      // mẹ mất 1/2 năng lượng
                int childEnergy = originalEnergy / 4;      // con nhận 1/4 năng lượng ban đầu
                if (childEnergy <= 0) return;
                this.setEnergy(parentAfter);
                int[] pos = walkable.get(RNG.nextInt(walkable.size()));
                Herbivore child = new Herbivore(
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
        return "h";
    }

    @Override
    public String getType() { return "Herbivore"; }
}
