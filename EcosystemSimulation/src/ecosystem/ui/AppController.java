package ecosystem.ui;

import javafx.scene.image.Image;
import ecosystem.Settings;
import ecosystem.logic.SimulationEngine;

import java.util.HashMap;
import java.util.Map;

public class AppController {
    private Settings settings;
    private SimulationEngine engine;
    private SelectionManager selectionManager;
    private Map<String, Image> iconMap = new HashMap<>();
    private Image gridBackgroundImage;
    private Image simRootBackgroundImage;
    private boolean useImageBackground = false;
    private boolean darkTheme = false;

    public AppController() {
        this.settings = new Settings();
        this.engine = new SimulationEngine(settings);
        this.selectionManager = new SelectionManager();
    }

    public void setSettings(Settings s) { this.settings = s; }

    public void setEngine(SimulationEngine e) { this.engine = e; }

    /** Save current simulation state to a plain text file. */
    public void saveToFile(java.io.File f) throws java.io.IOException {
        try (java.io.BufferedWriter w = new java.io.BufferedWriter(new java.io.FileWriter(f))) {
            w.write("ECO_SIM_V1\n");
            // settings (only a few keys necessary)
            w.write("gridWidth=" + settings.getGridWidth() + "\n");
            w.write("gridHeight=" + settings.getGridHeight() + "\n");
            w.write("initialPlants=" + settings.getInitialPlants() + "\n");
            w.write("initialHerbivores=" + settings.getInitialHerbivores() + "\n");
            w.write("initialCarnivores=" + settings.getInitialCarnivores() + "\n");
            w.write("herbivoreMoveCost=" + settings.getHerbivoreMoveCost() + "\n");
            w.write("herbivoreEatGain=" + settings.getHerbivoreEatGain() + "\n");
            w.write("carnivoreMoveCost=" + settings.getCarnivoreMoveCost() + "\n");
            w.write("carnivoreEatGain=" + settings.getCarnivoreEatGain() + "\n");
            w.write("plantEnergy=" + settings.getPlantEnergy() + "\n");
            w.write("plantGrowRate=" + settings.getPlantGrowRate() + "\n");
            w.write("\n");
            // engine state
            w.write("day=" + engine.getDay() + "\n");
            w.write("step=" + engine.getStep() + "\n");
            w.write("\n");
            // organisms: type,x,y,energy,age
            for (ecosystem.models.Organism o : engine.getGrid().getOrganisms()) {
                String line = String.join(",",
                    o.getType(),
                    Integer.toString(o.getX()),
                    Integer.toString(o.getY()),
                    Integer.toString(o.getEnergy()),
                    Integer.toString(o.getAge())
                );
                w.write(line + "\n");
            }
        }
    }

    /** Load simulation state from a file previously written by saveToFile. */
    public void loadFromFile(java.io.File f) throws java.io.IOException {
        java.util.List<String> lines = java.nio.file.Files.readAllLines(f.toPath());
        if (lines.isEmpty() || !lines.get(0).startsWith("ECO_SIM_V1")) throw new java.io.IOException("Unsupported save format");
        Settings s = new Settings();
        int idx = 1;
        // parse settings until blank line
        for (; idx < lines.size(); idx++) {
            String ln = lines.get(idx).trim();
            if (ln.isEmpty()) { idx++; break; }
            String[] kv = ln.split("=", 2);
            if (kv.length < 2) continue;
            String k = kv[0]; String v = kv[1];
            switch (k) {
                case "gridWidth": s.setGridWidth(Integer.parseInt(v)); break;
                case "gridHeight": s.setGridHeight(Integer.parseInt(v)); break;
                case "initialPlants": s.setInitialPlants(Integer.parseInt(v)); break;
                case "initialHerbivores": s.setInitialHerbivores(Integer.parseInt(v)); break;
                case "initialCarnivores": s.setInitialCarnivores(Integer.parseInt(v)); break;
                case "herbivoreMoveCost": s.setHerbivoreMoveCost(Integer.parseInt(v)); break;
                case "herbivoreEatGain": s.setHerbivoreEatGain(Integer.parseInt(v)); break;
                case "carnivoreMoveCost": s.setCarnivoreMoveCost(Integer.parseInt(v)); break;
                case "carnivoreEatGain": s.setCarnivoreEatGain(Integer.parseInt(v)); break;
                case "plantEnergy": s.setPlantEnergy(Integer.parseInt(v)); break;
                case "plantGrowRate": s.setPlantGrowRate(Double.parseDouble(v)); break;
            }
        }
        // parse day/step (step kept for backward compatibility, but day is canonical)
        int day = 1, step = 0;
        for (; idx < lines.size(); idx++) {
            String ln = lines.get(idx).trim();
            if (ln.isEmpty()) { idx++; break; }
            String[] kv = ln.split("=", 2);
            if (kv.length < 2) continue;
            if (kv[0].equals("day")) day = Integer.parseInt(kv[1]);
            if (kv[0].equals("step")) step = Integer.parseInt(kv[1]);
        }
        // remaining lines are organisms
        ecosystem.models.Grid g = new ecosystem.models.Grid(s);
        for (; idx < lines.size(); idx++) {
            String ln = lines.get(idx).trim();
            if (ln.isEmpty()) continue;
            String[] parts = ln.split(",");
            if (parts.length < 5) continue;
            String type = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int energy = Integer.parseInt(parts[3]);
            int age = Integer.parseInt(parts[4]);
            switch (type) {
                case "Plant": {
                    ecosystem.models.Plant p = new ecosystem.models.Plant(x, y, energy);
                    // no public way to set age; approximate by repeated increments? skip age exactness
                    g.addOrganism(p);
                    break;
                }
                case "Herbivore": {
                    ecosystem.models.Herbivore h = new ecosystem.models.Herbivore(
                        x, y, energy,
                        s.getHerbivoreMoveCost(), s.getHerbivoreEatGain(),
                        s.getHerbivoreReproduceThreshold(),
                        s.getHerbivoreMetabolismCost(),
                        s.getHerbivoreAbsorptionRate()
                    );
                    g.addOrganism(h);
                    break;
                }
                case "Carnivore": {
                    ecosystem.models.Carnivore c = new ecosystem.models.Carnivore(
                        x, y, energy,
                        s.getCarnivoreMoveCost(), s.getCarnivoreEatGain(),
                        s.getCarnivoreReproduceThreshold(),
                        s.getCarnivoreMetabolismCost(),
                        s.getCarnivoreAbsorptionRate()
                    );
                    g.addOrganism(c);
                    break;
                }
            }
        }
        // create engine from grid
        SimulationEngine e = new SimulationEngine(s, g);
        // Each tick is a day now; use saved "day" as the timeline position.
        e.setStep(day);
        // apply new settings and engine
        this.setSettings(s);
        this.setEngine(e);
    }

    public Settings getSettings() { return settings; }
    public SimulationEngine getEngine() { return engine; }
    public SelectionManager getSelectionManager() { return selectionManager; }
    public Map<String, Image> getIconMap() { return iconMap; }

    public Image getGridBackgroundImage() { return gridBackgroundImage; }
    public void setGridBackgroundImage(Image img) { this.gridBackgroundImage = img; }
    public Image getSimRootBackgroundImage() { return simRootBackgroundImage; }
    public void setSimRootBackgroundImage(Image img) { this.simRootBackgroundImage = img; }

    public boolean isUseImageBackground() { return useImageBackground; }
    public void setUseImageBackground(boolean v) { this.useImageBackground = v; }

    public boolean isDarkTheme() { return darkTheme; }
    public void setDarkTheme(boolean v) { this.darkTheme = v; }

    public void resetEngine() {
        this.engine = new SimulationEngine(settings);
    }
}
