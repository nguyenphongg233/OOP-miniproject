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

    public AppController() {
        this.settings = new Settings();
        this.engine = new SimulationEngine(settings);
        this.selectionManager = new SelectionManager();
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

    public void resetEngine() {
        this.engine = new SimulationEngine(settings);
    }
}
