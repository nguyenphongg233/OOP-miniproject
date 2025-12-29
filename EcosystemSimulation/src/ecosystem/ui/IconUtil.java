/**
 * Utility helpers for ensuring and applying application icons.
 */
package ecosystem.ui;

import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.io.File;
import javafx.scene.paint.Color;

public class IconUtil {
    public static void ensureAppIcons(Stage stage) throws Exception {
        java.nio.file.Path iconsDir = java.nio.file.Path.of("EcosystemSimulation", "icons");
        if (!java.nio.file.Files.exists(iconsDir)) java.nio.file.Files.createDirectories(iconsDir);
        File ic256 = iconsDir.resolve("app_icon_256.png").toFile();
        File ic48 = iconsDir.resolve("app_icon_48.png").toFile();
        if (!ic256.exists() || !ic48.exists()) {
            generateIcon(ic256, 256, Color.web("#69bdac"), Color.web("#ffffff"));
            generateIcon(ic48, 48, Color.web("#69bdac"), Color.web("#ffffff"));
        }
        try {
            stage.getIcons().clear();
            stage.getIcons().add(new Image(ic256.toURI().toString()));
            stage.getIcons().add(new Image(ic48.toURI().toString()));
        } catch (Exception ex) {
            // ignore
        }
    }

    private static void generateIcon(File out, int size, Color bg, Color fg) throws Exception {
        WritableImage img = new WritableImage(size, size);
        PixelWriter pw = img.getPixelWriter();
        for (int x = 0; x < size; x++) for (int y = 0; y < size; y++) pw.setColor(x, y, bg);
        javafx.scene.canvas.Canvas c = new javafx.scene.canvas.Canvas(size, size);
        javafx.scene.canvas.GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(bg);
        g.fillRect(0,0,size,size);
        g.setFill(fg);
        g.setGlobalAlpha(0.95);
        g.fillOval(size*0.12, size*0.12, size*0.76, size*0.76);
        g.setFill(bg);
        g.fillOval(size*0.38, size*0.20, size*0.34, size*0.60);
        g.setFill(fg.brighter());
        g.fillOval(size*0.42, size*0.26, size*0.20, size*0.48);
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(javafx.scene.paint.Color.TRANSPARENT);
        WritableImage snap = c.snapshot(params, null);
        java.awt.image.BufferedImage bimg = SwingFXUtils.fromFXImage(snap, null);
        ImageIO.write(bimg, "png", out);
    }
}
