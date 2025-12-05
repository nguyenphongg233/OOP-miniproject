    // ...existing code...
package ecosystem.logic;

import ecosystem.Settings;
import ecosystem.models.Grid;

public class SimulationEngine {
        // Explicit update methods for UI sync
    
    private int day = 1;
    private int step = 0;
    public Grid grid;
    private boolean verbose = true; // if true, print console output on each tick

    public void update_day() {
        // Recalculate day based on step
        this.day = (step / 10) + 1;
    }

    public void update_organ() {
        // No-op: counts() always reflects current grid
        // But can be used to trigger UI refresh if needed

        

    }
    public SimulationEngine() {
        this(new Settings(), true);
    }

    // Construct engine from Settings (used by GUI). Verbose=false to avoid console spam.
    public SimulationEngine(Settings s) {
        this(s, false);
    }

    public SimulationEngine(Settings s, boolean verbose) {
        this.grid = new Grid(s);
        this.verbose = verbose;
        this.grid.setHerbivoreReproduceThreshold(s.herbivoreReproduceThreshold);
        this.grid.setCarnivoreReproduceThreshold(s.carnivoreReproduceThreshold);
        this.grid.populateBasic(
            s.initialPlants,
            s.initialHerbivores,
            s.initialCarnivores,
            s.herbivoreStartEnergy,
            s.herbivoreMoveCost,
            s.herbivoreEatGain,
            s.carnivoreStartEnergy,
            s.carnivoreMoveCost,
            s.carnivoreEatGain
        );
    }

    public void tick() {
        this.grid.stepAll();
        step++;
        update_day();
        update_organ();
        if (verbose) renderConsole();
    }
    public int getDay() {
        return day;
    }
    public int getStep() {
        return step;
    }

    private void renderConsole() {
        for (String r : grid.asciiGrid()) System.out.println(r);
        java.util.Map<String, Integer> counts = grid.counts();
        System.out.println("Plants: " + counts.getOrDefault("Plant", 0) + " | Herbivores: " + counts.getOrDefault("Herbivore", 0) + " | Carnivores: " + counts.getOrDefault("Carnivore", 0));
    }

    public java.util.Map<String, Integer> counts() {
        return grid.counts();
    }
}
