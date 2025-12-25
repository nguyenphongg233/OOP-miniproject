package ecosystem.ui;

import javafx.scene.image.Image;
import java.nio.file.Files;
import java.nio.file.Path;

public class IconLoader {
    public static void loadIcons(AppController c) {
        String[] names = {"Plant", "Herbivore", "Carnivore"};
        Path[] candidates = new Path[] {
            Path.of("EcosystemSimulation", "icons"),
            Path.of("icons")
        };
        for (String n : names) {
            Image found = null;
            for (Path base : candidates) {
                Path p = base.resolve(n + ".png");
                if (Files.exists(p)) {
                    try { found = new Image(p.toUri().toString()); break; } catch (Exception ex) {}
                }
                p = base.resolve(n.toLowerCase() + ".png");
                if (Files.exists(p)) {
                    try { found = new Image(p.toUri().toString()); break; } catch (Exception ex) {}
                }
            }
            if (found != null) c.getIconMap().put(n, found);
        }
        Path bgPath = Path.of("EcosystemSimulation", "icons", "grid_background.png");
        if (Files.exists(bgPath)) {
            try { c.setGridBackgroundImage(new Image(bgPath.toUri().toString())); } catch (Exception ex) { c.setGridBackgroundImage(null); }
        }
        // Original naming for simulation root background
        Path simBgPath = Path.of("EcosystemSimulation", "icons", "sim_background.png");
        if (Files.exists(simBgPath)) {
            try { c.setSimRootBackgroundImage(new Image(simBgPath.toUri().toString())); } catch (Exception ex) { c.setSimRootBackgroundImage(null); }
        }
    }
}
