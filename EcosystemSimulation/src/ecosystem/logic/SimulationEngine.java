    // ...existing code...
package ecosystem.logic;

import ecosystem.models.Settings;
import ecosystem.models.Grid;

public class SimulationEngine {
        // Explicit update methods for UI sync
    
    private int day = 1;
    // "step" is now equivalent to day; we keep it only for save/load compatibility.
    private int step = 0;
    private Grid grid;
    private boolean verbose = true; // if true, print console output on each tick

    public void update_day() {
        // Each tick is now one full day; no sub-steps.
        this.day = step;
    }

    public Grid getGrid() { return grid; }

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
        this.grid = new Grid(s.getGridWidth(), s.getGridHeight(), s);
        this.verbose = verbose;
        this.grid.populateBasic(
            s.getInitialPlants(),
            s.getInitialHerbivores(),
            s.getInitialCarnivores(),
            s.getHerbivoreStartEnergy(),
            s.getHerbivoreMoveCost(),
            s.getHerbivoreEatGain(),
            s.getCarnivoreStartEnergy(),
            s.getCarnivoreMoveCost(),
            s.getCarnivoreEatGain(),
            s.getHerbivoreReproduceThreshold(),
            s.getHerbivoreMetabolismCost(),
            s.getHerbivoreAbsorptionRate(),
            s.getCarnivoreReproduceThreshold(),
            s.getCarnivoreMetabolismCost(),
            s.getCarnivoreAbsorptionRate()
        );
    }

    // Construct engine from Settings with an existing Grid (used for loading saved state)
    public SimulationEngine(Settings s, ecosystem.models.Grid grid, boolean verbose) {
        this.grid = grid;
        this.verbose = verbose;
    }

    public SimulationEngine(Settings s, ecosystem.models.Grid grid) {
        this(s, grid, false);
    }

    public void tick() {
        // One tick == one simulation day
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

    public void setStep(int s) {
        this.step = s;
        // Keep day and step in sync (no 10-steps-per-day logic anymore)
        this.day = s;
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
