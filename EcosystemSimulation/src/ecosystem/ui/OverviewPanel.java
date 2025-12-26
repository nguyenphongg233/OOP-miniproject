package ecosystem.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import ecosystem.models.Organism;

public class OverviewPanel {
    private final AppController controller;
    private final VBox root;
    private final Canvas overviewCanvas;
    private final IntegerProperty overviewGridW = new SimpleIntegerProperty(0);
    private final IntegerProperty overviewGridH = new SimpleIntegerProperty(0);

    public OverviewPanel(AppController controller) {
        this.controller = controller;
        root = new VBox(10);
        root.setPadding(new Insets(8));
        root.setPrefWidth(180);

        overviewCanvas = new Canvas(160, 160);
        overviewCanvas.widthProperty().bind(root.widthProperty().subtract(16));
        overviewCanvas.heightProperty().bind(Bindings.createDoubleBinding(() -> {
            double w = overviewCanvas.getWidth();
            int gw = Math.max(1, overviewGridW.get());
            int gh = Math.max(1, overviewGridH.get());
            return w * ((double) gh / (double) gw);
        }, overviewCanvas.widthProperty(), overviewGridW, overviewGridH));
        overviewCanvas.getGraphicsContext2D().setFill(Color.LIGHTGRAY);
        overviewCanvas.getGraphicsContext2D().fillRect(0, 0, 160, 160);
        overviewCanvas.widthProperty().addListener((o, oldV, newV) -> drawOverview());
        overviewCanvas.heightProperty().addListener((o, oldV, newV) -> drawOverview());

        Label overviewLabel = new Label("Overview");
        TreeItem<String> rootItem = new TreeItem<>("Layers");
        TreeItem<String> envItem = new TreeItem<>("Environment");
        envItem.getChildren().add(new TreeItem<>("Sunlight"));
        envItem.getChildren().add(new TreeItem<>("Minerals"));
        TreeItem<String> orgItem = new TreeItem<>("Organisms");
        orgItem.getChildren().add(new TreeItem<>("Plants"));
        orgItem.getChildren().add(new TreeItem<>("Herbivores"));
        orgItem.getChildren().add(new TreeItem<>("Carnivores"));
        TreeItem<String> dnaItem = new TreeItem<>("DNA");
        dnaItem.getChildren().add(new TreeItem<>("MinMass"));
        dnaItem.getChildren().add(new TreeItem<>("MaxMass"));
        dnaItem.getChildren().add(new TreeItem<>("GrowthRate"));
        rootItem.getChildren().addAll(envItem, orgItem, dnaItem);
        TreeView<String> treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(true);

        root.getChildren().addAll(overviewLabel, overviewCanvas, treeView);
        overviewGridW.set(controller.getSettings().getGridWidth());
        overviewGridH.set(controller.getSettings().getGridHeight());
        drawOverview();
    }

    public VBox getRoot() { return root; }

    public void setGridSize(int width, int height) {
        overviewGridW.set(width);
        overviewGridH.set(height);
        drawOverview();
    }

    public void drawOverview() {
        if (overviewCanvas == null || controller.getEngine() == null) return;
        GraphicsContext g = overviewCanvas.getGraphicsContext2D();
        int cols = controller.getEngine().getGrid().getWidth();
        int rows = controller.getEngine().getGrid().getHeight();
        double w = overviewCanvas.getWidth();
        double h = overviewCanvas.getHeight();
        if (w <= 0 || h <= 0 || cols <= 0 || rows <= 0) return;
        g.setFill(Color.web("#f0f0f0"));
        g.fillRect(0, 0, w, h);
        double cellW = w / cols;
        double cellH = h / rows;
        double scale = Math.min(cellW, cellH);
        int drawCols = (int)Math.min(cols, Math.max(1, Math.floor(w / scale)));
        int drawRows = (int)Math.min(rows, Math.max(1, Math.floor(h / scale)));
        double drawCellW = w / drawCols;
        double drawCellH = h / drawRows;
        for (int ry = 0; ry < drawRows; ry++) {
            for (int rx = 0; rx < drawCols; rx++) {
                int gx = (int)((double)rx * cols / drawCols);
                int gy = (int)((double)ry * rows / drawRows);
                java.util.List<Organism> objs = controller.getEngine().getGrid().organismsAt(gx, gy);
                if (objs.isEmpty()) g.setFill(Color.web("#e9efe9"));
                else {
                    String type = objs.get(0).getType();
                    switch (type) {
                        case "Plant": g.setFill(Color.web("#7fbf7f")); break;
                        case "Herbivore": g.setFill(Color.DARKGRAY); break;
                        case "Carnivore": g.setFill(Color.BLACK); break;
                        default: g.setFill(Color.GRAY); break;
                    }
                }
                g.fillRect(rx * drawCellW, ry * drawCellH, drawCellW, drawCellH);
            }
        }
    }
}
