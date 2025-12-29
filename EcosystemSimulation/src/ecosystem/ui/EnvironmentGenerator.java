/**
 * Generates per-cell environment maps (colors + terrain) used for
 * background rendering and terrain constraints.
 */
package ecosystem.ui;

import javafx.scene.paint.Color;

import ecosystem.models.Grid;

public class EnvironmentGenerator {

    /** Container for both the rendered color map and the logical terrain map. */
    public static class EnvironmentData {
        public final Color[][] colors;
        public final int[][] terrain;

        public EnvironmentData(Color[][] colors, int[][] terrain) {
            this.colors = colors;
            this.terrain = terrain;
        }
    }

    /**
     * Generate both a visual environment map (colors) and a logical terrain
     * classification for each cell. Terrain types follow Grid.TERRAIN_* constants.
     */
    public static EnvironmentData generateEnvironment(int cols, int rows) {
        if (cols <= 0 || rows <= 0) {
            return new EnvironmentData(new Color[0][0], new int[0][0]);
        }

        double[][] noise = new double[cols][rows];
        java.util.Random rnd = new java.util.Random(System.currentTimeMillis());
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                noise[x][y] = rnd.nextDouble();
            }
        }

        int smoothPasses = 4;
        for (int pass = 0; pass < smoothPasses; pass++) {
            double[][] tmp = new double[cols][rows];
            for (int x = 0; x < cols; x++) {
                for (int y = 0; y < rows; y++) {
                    double sum = 0; int cnt = 0;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            int nx = x + dx, ny = y + dy;
                            if (nx >= 0 && nx < cols && ny >= 0 && ny < rows) { sum += noise[nx][ny]; cnt++; }
                        }
                    }
                    tmp[x][y] = sum / Math.max(1, cnt);
                }
            }
            noise = tmp;
        }

        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                min = Math.min(min, noise[x][y]);
                max = Math.max(max, noise[x][y]);
            }
        }
        double range = Math.max(1e-6, max - min);

        Color[][] colors = new Color[cols][rows];
        int[][] terrain = new int[cols][rows];

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                double v = (noise[x][y] - min) / range;
                v = Math.pow(v, 1.0 + rnd.nextDouble() * 0.4);

                Color c;
                int terr;

                // Same visual thresholds as before, but also assign terrain type
                if (v < 0.25) {
                    // Deep/shallow water (biển, hồ)
                    double t = v / 0.25;
                    c = Color.color(0.02 + 0.05 * t, 0.15 + 0.25 * t, 0.5 + 0.4 * t);
                    terr = Grid.TERRAIN_WATER;
                } else if (v < 0.35) {
                    // Sand / beach
                    double t = (v - 0.25) / 0.10;
                    c = Color.color(0.76 + 0.10 * t, 0.70 + 0.08 * t, 0.50 + 0.02 * t);
                    terr = Grid.TERRAIN_SAND;
                } else if (v < 0.75) {
                    // Grassland (vùng màu xanh có cỏ)
                    double t = (v - 0.35) / 0.40;
                    c = Color.color(0.30 + 0.30 * t, 0.54 + 0.28 * t, 0.20 + 0.10 * t);
                    terr = Grid.TERRAIN_GRASS;
                } else {
                    // Rock / mountain
                    double t = Math.min(1.0, (v - 0.75) / 0.25);
                    c = Color.color(0.50 + 0.25 * t, 0.45 + 0.25 * t, 0.36 + 0.24 * t);
                    terr = Grid.TERRAIN_ROCK;
                }

                double n = (rnd.nextDouble() - 0.5) * 0.06;
                double r = Math.max(0, Math.min(1, c.getRed() + n));
                double g = Math.max(0, Math.min(1, c.getGreen() + n));
                double b = Math.max(0, Math.min(1, c.getBlue() + n));
                colors[x][y] = Color.color(r, g, b);
                terrain[x][y] = terr;
            }
        }

        return new EnvironmentData(colors, terrain);
    }

    /** Backwards-compatible helper for callers that only need colors. */
    public static Color[][] generateEnvMap(int cols, int rows) {
        return generateEnvironment(cols, rows).colors;
    }
}
