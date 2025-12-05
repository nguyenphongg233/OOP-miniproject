
package ecosystem.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import ecosystem.logic.SimulationEngine;

public class SimulationScreen {
    private SimulationEngine engine;
    private int cellSize = 20;
    private Timeline timeline;
    private Scene scene;
    private Canvas canvas;
    private Label statsLabel;
    private Label detailLabel;
    private Runnable updateSummary;
    private Runnable updateDetail;

    public SimulationScreen(SimulationEngine engine) { this.engine = engine; }

    private void setupUI() {
        loadGridBackground();
        MenuBar menuBar = createMenuBar();
        VBox leftPanel = createLeftPanel();
        canvas = createCanvas();
        VBox rightPanel = createRightPanel();
        HBox controls = createControls();
        BorderPane simRoot = new BorderPane();
        simRoot.setBackground(new Background(new BackgroundFill(
                Color.BEIGE, CornerRadii.EMPTY, Insets.EMPTY
        )));
        simRoot.setTop(menuBar);
        simRoot.setLeft(leftPanel);
        simRoot.setCenter(canvas);
        simRoot.setRight(rightPanel);
        simRoot.setBottom(controls);
        scene = new Scene(simRoot, 900, 700);
        setupEventHandlers(controls);
    }
// removed extra closing brace





    // --- Event Handlers ---

    // --- Event Handlers ---
    private void setupEventHandlers(HBox controls) {
        // Lấy các button từ controls
        Button startBtn = (Button) controls.getChildren().get(0);
        Button pauseBtn = (Button) controls.getChildren().get(1);
        Button stepBtn = (Button) controls.getChildren().get(2);
        Button resetBtn = (Button) controls.getChildren().get(3);
        //Button settingsBtn = (Button) controls.getChildren().get(4);
        //Button backBtn = (Button) controls.getChildren().get(5);
        Slider speed = (Slider) controls.getChildren().get(8);

        timeline = new Timeline(new KeyFrame(Duration.millis(speed.getValue()), e -> {
            engine.tick();
            drawGrid();
            if (statsLabel != null) statsLabel.setText(formatCounts());
            if (updateSummary != null) updateSummary.run();
            if (updateDetail != null) updateDetail.run();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        speed.valueProperty().addListener((obs, oldV, newV) -> timeline.setRate(1000.0 / newV.doubleValue()));

        startBtn.setOnAction(e -> timeline.play());
        pauseBtn.setOnAction(e -> timeline.pause());
        stepBtn.setOnAction(e -> {
            engine.tick();
            drawGrid();
            if (statsLabel != null) statsLabel.setText(formatCounts());
            if (updateSummary != null) updateSummary.run();
            if (updateDetail != null) updateDetail.run();
        });
        resetBtn.setOnAction(e -> {
            timeline.stop();
            engine = new SimulationEngine(new ecosystem.Settings());
            canvas.setWidth(engine.grid.getWidth() * cellSize);
            canvas.setHeight(engine.grid.getHeight() * cellSize);
            drawGrid();
            if (statsLabel != null) statsLabel.setText(formatCounts());
            if (updateSummary != null) updateSummary.run();
            if (updateDetail != null) updateDetail.run();
        });
        canvas.widthProperty().addListener((o, oldV, newV) -> drawGrid());
        canvas.heightProperty().addListener((o, oldV, newV) -> drawGrid());
        CheckBox autoResize = (CheckBox) controls.getChildren().get(6);
        autoResize.selectedProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                double rightPanelWidth = 220;
                double bottomPanel = 120;
                canvas.widthProperty().bind(scene.widthProperty().subtract(rightPanelWidth));
                canvas.heightProperty().bind(scene.heightProperty().subtract(bottomPanel));
            } else {
                canvas.widthProperty().unbind();
                canvas.heightProperty().unbind();
                canvas.setWidth(engine.grid.getWidth() * cellSize);
                canvas.setHeight(engine.grid.getHeight() * cellSize);
            }
            drawGrid();
        });
        canvas.setOnMouseClicked(e -> {
            int x = (int)(e.getX() / cellSize);
            int y = (int)(e.getY() / cellSize);
            if (x >= 0 && x < engine.grid.getWidth() && y >= 0 && y < engine.grid.getHeight()) {
                java.util.List<ecosystem.models.Organism> objs = engine.grid.organismsAt(x, y);
                if (!objs.isEmpty()) {
                    ecosystem.models.Organism o = objs.get(0);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Type: ").append(o.getType()).append("\n");
                    sb.append("Position: (").append(o.getX()).append(", ").append(o.getY()).append(")\n");
                    sb.append("Age: ").append(o.getAge()).append("\n");
                    sb.append("Energy: ").append(o.getEnergy()).append("\n");
                    sb.append("Alive: ").append(o.isAlive() ? "Yes" : "No").append("\n");
                    detailLabel.setText(sb.toString());
                    updateDetail = () -> detailLabel.setText(sb.toString());
                } else {
                    detailLabel.setText("No organism at this cell.");
                    updateDetail = () -> detailLabel.setText("No organism at this cell.");
                }
            }
        });
    }

    // --- Helpers (stubs) ---
    private java.util.Map<String, javafx.scene.image.Image> iconMap = new java.util.HashMap<>();

    private void drawGrid() {
        if (canvas == null || engine == null || engine.grid == null) return;
        javafx.scene.canvas.GraphicsContext g = canvas.getGraphicsContext2D();
        int cols = engine.grid.getWidth();
        int rows = engine.grid.getHeight();
        double w = canvas.getWidth() / cols;
        double h = canvas.getHeight() / rows;
        if (useImageBackground && gridBackgroundImage != null) {
            g.drawImage(gridBackgroundImage, 0, 0, canvas.getWidth(), canvas.getHeight());
        } else {
            g.setFill(Color.BEIGE);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                java.util.List<ecosystem.models.Organism> objs = engine.grid.organismsAt(x, y);
                if (objs.isEmpty()) continue;
                String name = objs.get(0).getType();
                javafx.scene.image.Image img = iconMap.get(name);
                if (img != null) {
                    g.drawImage(img, x * w, y * h, w, h);
                } else {
                    switch (name) {
                        case "Plant": g.setFill(Color.GREEN); break;
                        case "Herbivore": g.setFill(Color.ORANGE); break;
                        case "Carnivore": g.setFill(Color.RED); break;
                        default: g.setFill(Color.BLACK); break;
                    }
                    g.fillRect(x * w, y * h, w, h);
                }
            }
        }
        // Explicitly update engine state after grid update
        engine.update_day();
        engine.update_organ();
    }

    private String formatCounts() {
        java.util.Map<String, Integer> counts = engine.counts();
        return String.format("Plants: %d  Herbivores: %d  Carnivores: %d",
            counts.getOrDefault("Plant", 0),
            counts.getOrDefault("Herbivore", 0),
            counts.getOrDefault("Carnivore", 0));
    }

    // --- UI container creation methods (stubs, to be implemented) ---
    // --- State for background image and toggle ---
    private javafx.scene.image.Image gridBackgroundImage = null;
    private boolean useImageBackground = false;

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        Menu menuWorld = new Menu("World");
        Menu menuHelp = new Menu("Help");
        Menu menuView = new Menu("View");
        RadioMenuItem bgImgItem = new RadioMenuItem("Grid Background: Image (icons/grid_background.png)");
        RadioMenuItem bgColorItem = new RadioMenuItem("Grid Background: Beige");
        ToggleGroup bgGroup = new ToggleGroup();
        bgImgItem.setToggleGroup(bgGroup);
        bgColorItem.setToggleGroup(bgGroup);
        if (gridBackgroundImage != null) {
            bgImgItem.setSelected(true);
            useImageBackground = true;
        } else {
            bgColorItem.setSelected(true);
            useImageBackground = false;
        }
        menuView.getItems().addAll(bgImgItem, bgColorItem);
        menuBar.getMenus().addAll(menuFile, menuWorld, menuView, menuHelp);
        bgImgItem.setOnAction(e -> { useImageBackground = true; drawGrid(); });
        bgColorItem.setOnAction(e -> { useImageBackground = false; drawGrid(); });
        return menuBar;
    }
    private VBox createLeftPanel() {
        // TODO: implement left panel creation
        return new VBox();
    }
    private VBox createRightPanel() {
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(8));
        rightPanel.setPrefWidth(220);
        Label orgListLabel = new Label("Organisms");
        ListView<String> orgListView = new ListView<>();
        orgListView.setPrefHeight(120);
        Label propLabel = new Label("Properties");
        VBox propBox = new VBox(8);
        propBox.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 8; -fx-border-radius: 8; -fx-background-radius: 8;");
        // Property labels
        Label dayLabel = new Label();
        Label plantLabel = new Label();
        Label herbLabel = new Label();
        Label carnLabel = new Label();
        Label orgLabel = new Label();
        detailLabel = new Label("Click a cell to view organism details");
        detailLabel.setWrapText(true);
        propBox.getChildren().addAll(dayLabel, plantLabel, herbLabel, carnLabel, orgLabel, new Separator(), detailLabel);
        rightPanel.getChildren().addAll(orgListLabel, orgListView, propLabel, propBox);
        // Helper to update summary info
        updateSummary = () -> {
            java.util.Map<String, Integer> counts = engine.counts();
            dayLabel.setText("Day: " + engine.getDay() + " (Step: " + engine.getStep() + ")");
            plantLabel.setText("Plants: " + counts.getOrDefault("Plant", 0));
            herbLabel.setText("Herbivores: " + counts.getOrDefault("Herbivore", 0));
            carnLabel.setText("Carnivores: " + counts.getOrDefault("Carnivore", 0));
            int total = 0;
            for (int v : counts.values()) total += v;
            orgLabel.setText("Total Organisms: " + total);
        };
        updateSummary.run();
        // Helper to update organism detail info (clears if none selected)
        updateDetail = () -> {
            detailLabel.setText("Click a cell to view organism details");
        };
        return rightPanel;
    }
    private HBox createControls() {
        Button startBtn = new Button("Start");
        Button pauseBtn = new Button("Pause");
        Button stepBtn = new Button("Step");
        Button resetBtn = new Button("Reset");
        Button settingsBtn = new Button("Settings");
        Button backBtn = new Button("Back to Menu");
        CheckBox autoResize = new CheckBox("Auto-resize");
        Slider speed = new Slider(10, 1000, 200);
        speed.setShowTickLabels(true);
        speed.setShowTickMarks(true);
        HBox controls = new HBox(8, startBtn, pauseBtn, stepBtn, resetBtn, settingsBtn, backBtn, autoResize, new Label("Delay ms:"), speed);
        controls.setPadding(new Insets(8));
        controls.setStyle("-fx-background-color: #ffffffcc; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, #b2ebf2, 8, 0.2, 0, 2)");
        String btnStyle = "-fx-background-radius: 16; -fx-effect: dropshadow(gaussian, #80deea, 8, 0.3, 0, 2); -fx-font-size: 14px; -fx-padding: 8 20;";
        startBtn.setStyle(btnStyle);
        pauseBtn.setStyle(btnStyle);
        stepBtn.setStyle(btnStyle);
        resetBtn.setStyle(btnStyle);
        settingsBtn.setStyle(btnStyle);
        backBtn.setStyle(btnStyle);
        return controls;
    }
    private Canvas createCanvas() {
        // Sử dụng engine.grid để lấy kích thước
        return new Canvas(engine.grid.getWidth() * cellSize, engine.grid.getHeight() * cellSize);
    }
    private void loadGridBackground() {
        java.nio.file.Path bgPath = java.nio.file.Path.of("EcosystemSimulation", "icons", "grid_background.png");
        if (java.nio.file.Files.exists(bgPath)) {
            try {
                gridBackgroundImage = new javafx.scene.image.Image(bgPath.toUri().toString());
            } catch (Exception ex) { gridBackgroundImage = null; }
        }
    }
}



