package ecosystem.ui;

import ecosystem.logic.SimulationEngine;

public class StatsPanel {
    private SimulationEngine engine;

    public StatsPanel(SimulationEngine engine) { this.engine = engine; }

    public void show() {
        java.util.Map<String, Integer> counts = engine.counts();
        System.out.println("Stats -> Plants: " + counts.getOrDefault("Plant", 0) + ", Herbivores: " + counts.getOrDefault("Herbivore", 0) + ", Carnivores: " + counts.getOrDefault("Carnivore", 0));
    }
}
