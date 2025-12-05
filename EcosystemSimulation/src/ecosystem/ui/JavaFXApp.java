// import javafx.scene.image.ImageView; // not needed
// import javafx.stage.FileChooser; // not needed
package ecosystem.ui;

import javafx.geometry.Pos;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import ecosystem.Settings;
import ecosystem.logic.SimulationEngine;
import ecosystem.models.Organism;
import javafx.scene.image.Image;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class JavaFXApp extends Application {

    // Instance variables
    private SimulationEngine engine;
    private Settings settings;
    private int cellSize = 20;
    private Timeline timeline;
    private Scene menuScene;
    private Scene simulationScene;
    private Canvas canvas;
    private Label statsLabel;
    private Map<String, Image> iconMap = new HashMap<>();
    private Image gridBackgroundImage = null;
    private boolean useImageBackground = false;
    // For property panel
    private Label detailLabel;
    private Runnable updateSummary;
    private Runnable updateDetail;

    @Override
    public void start(Stage primaryStage) {
        // Load grid background image if exists
        Path bgPath = Path.of("EcosystemSimulation", "icons", "grid_background.png");
        if (Files.exists(bgPath)) {
            try {
                gridBackgroundImage = new Image(bgPath.toUri().toString());
            } catch (Exception ex) { gridBackgroundImage = null; }
        }

        // Khung layout mới
        settings = new Settings();
        engine = new SimulationEngine(settings);
        loadIcons();

        // MenuBar (Top)
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        Menu menuWorld = new Menu("World");
        Menu menuHelp = new Menu("Help");
        // Add background selection
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

        // Left: Overview + TreeView
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(8));
        leftPanel.setPrefWidth(180);
        Canvas overviewCanvas = new Canvas(120, 120);
        overviewCanvas.getGraphicsContext2D().setFill(Color.LIGHTGRAY);
        overviewCanvas.getGraphicsContext2D().fillRect(0, 0, 120, 120);
        Label overviewLabel = new Label("Overview");
        TreeItem<String> rootItem = new TreeItem<>("Layers");
        TreeItem<String> envItem = new TreeItem<>("Environment");
        envItem.getChildren().add(new TreeItem<>("Water"));
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
        dnaItem.getChildren().add(new TreeItem<>("DesireForWater"));
        dnaItem.getChildren().add(new TreeItem<>("DesireForSunlight"));
        dnaItem.getChildren().add(new TreeItem<>("DesireForMinerals"));
        dnaItem.getChildren().add(new TreeItem<>("DesireForPlants"));
        dnaItem.getChildren().add(new TreeItem<>("DesireForAnimals"));
        dnaItem.getChildren().add(new TreeItem<>("CostToMaintain"));
        dnaItem.getChildren().add(new TreeItem<>("CostToGrow"));
        dnaItem.getChildren().add(new TreeItem<>("CostToReprod"));
        rootItem.getChildren().add(envItem);
        rootItem.getChildren().add(orgItem);
        rootItem.getChildren().add(dnaItem);
        TreeView<String> treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(true);
        leftPanel.getChildren().addAll(overviewLabel, overviewCanvas, treeView);

        // Center: Canvas lớn
        canvas = new Canvas(engine.grid.getWidth() * cellSize, engine.grid.getHeight() * cellSize);
        drawGrid();

        // Right: ListView + Properties
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

        // Bottom: Control Panel (EcoSim style)
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
        // Style buttons: bo tròn, shadow
        String btnStyle = "-fx-background-radius: 16; -fx-effect: dropshadow(gaussian, #80deea, 8, 0.3, 0, 2); -fx-font-size: 14px; -fx-padding: 8 20;";
        startBtn.setStyle(btnStyle);
        pauseBtn.setStyle(btnStyle);
        stepBtn.setStyle(btnStyle);
        resetBtn.setStyle(btnStyle);
        settingsBtn.setStyle(btnStyle);
        backBtn.setStyle(btnStyle);

        // BorderPane layout
        BorderPane simRoot = new BorderPane();
        simRoot.setBackground(new Background(new BackgroundFill(
                Color.BEIGE, CornerRadii.EMPTY, Insets.EMPTY
        )));
        simRoot.setTop(menuBar);
        simRoot.setLeft(leftPanel);
        simRoot.setRight(rightPanel);
        BorderPane bottomPane = new BorderPane();
        bottomPane.setCenter(canvas);
        bottomPane.setBottom(controls);
        
        simRoot.setCenter(bottomPane);

        timeline = new Timeline(new KeyFrame(Duration.millis(speed.getValue()), e -> {
            engine.tick();
            drawGrid();
            statsLabel.setText(formatCounts());
            updateSummary.run();
            updateDetail.run(); // Clear detail on step
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        speed.valueProperty().addListener((obs, oldV, newV) -> timeline.setRate(1000.0 / newV.doubleValue()));

        startBtn.setOnAction(e -> timeline.play());
        pauseBtn.setOnAction(e -> timeline.pause());
        stepBtn.setOnAction(e -> {
            engine.tick();
            drawGrid();
            statsLabel.setText(formatCounts());
            updateSummary.run();
            updateDetail.run();
        });
        resetBtn.setOnAction(e -> {
            timeline.stop();
            engine = new SimulationEngine(settings);
            canvas.setWidth(engine.grid.getWidth() * cellSize);
            canvas.setHeight(engine.grid.getHeight() * cellSize);
            drawGrid();
            statsLabel.setText(formatCounts());
            updateSummary.run();
            updateDetail.run();
        });
        settingsBtn.setOnAction(e -> openSettingsDialog(primaryStage, statsLabel));
        backBtn.setOnAction(e -> {
            timeline.stop();
            primaryStage.setScene(menuScene);
            primaryStage.setTitle("Ecosystem Simulation — Main Menu");
        });
        canvas.widthProperty().addListener((o, oldV, newV) -> drawGrid());
        canvas.heightProperty().addListener((o, oldV, newV) -> drawGrid());
        autoResize.selectedProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                double rightPanelWidth = 220;
                double bottomPanel = 120;
                canvas.widthProperty().bind(simRoot.widthProperty().subtract(rightPanelWidth));
                canvas.heightProperty().bind(simRoot.heightProperty().subtract(bottomPanel));
            } else {
                canvas.widthProperty().unbind();
                canvas.heightProperty().unbind();
                canvas.setWidth(engine.grid.getWidth() * cellSize);
                canvas.setHeight(engine.grid.getHeight() * cellSize);
            }
            drawGrid();
        });

        // Handle click on canvas to show organism details
        canvas.setOnMouseClicked(e -> {
            int x = (int)(e.getX() / cellSize);
            int y = (int)(e.getY() / cellSize);
            if (x >= 0 && x < engine.grid.getWidth() && y >= 0 && y < engine.grid.getHeight()) {
                java.util.List<Organism> objs = engine.grid.organismsAt(x, y);
                if (!objs.isEmpty()) {
                    Organism o = objs.get(0);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Type: ").append(o.getType()).append("\n");
                    sb.append("Position: (").append(o.getX()).append(", ").append(o.getY()).append(")\n");
                    sb.append("Age: ").append(o.getAge()).append("\n");
                    sb.append("Energy: ").append(o.getEnergy()).append("\n");
                    // Add more info if available
                    sb.append("Alive: ").append(o.isAlive() ? "Yes" : "No").append("\n");
                    detailLabel.setText(sb.toString());
                    // Update detail updater to keep this info until next step
                    updateDetail = () -> detailLabel.setText(sb.toString());
                } else {
                    detailLabel.setText("No organism at this cell.");
                    updateDetail = () -> detailLabel.setText("No organism at this cell.");
                }
            }
        });

        simulationScene = new Scene(simRoot, 900, 700);

        // --- Menu Scene ---
        MainMenu menu = new MainMenu(primaryStage,
            () -> {
                primaryStage.setScene(simulationScene);
                primaryStage.setTitle("Ecosystem Simulation (JavaFX)");
            },
            () -> openSettingsDialog(primaryStage, statsLabel)
        );
        menu.show();
        menuScene = primaryStage.getScene();
    }

    private void openSettingsDialog(Stage owner, Label statsLabel) {
        Stage dlg = new Stage();
        dlg.initOwner(owner);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Settings");
        TextField gridW = new TextField(String.valueOf(settings.gridWidth));
        TextField gridH = new TextField(String.valueOf(settings.gridHeight));

        TextField plants = new TextField(String.valueOf(settings.initialPlants));
        TextField herbs = new TextField(String.valueOf(settings.initialHerbivores));
        TextField carn = new TextField(String.valueOf(settings.initialCarnivores));

        TextField plantEnergy = new TextField(String.valueOf(settings.plantEnergy));
        TextField plantGrow = new TextField(String.valueOf(settings.plantGrowRate));

        TextField hEnergy = new TextField(String.valueOf(settings.herbivoreStartEnergy));
        TextField hMove = new TextField(String.valueOf(settings.herbivoreMoveCost));
        TextField hEat = new TextField(String.valueOf(settings.herbivoreEatGain));
        TextField hRepro = new TextField(String.valueOf(settings.herbivoreReproduceThreshold));

        TextField cEnergy = new TextField(String.valueOf(settings.carnivoreStartEnergy));
        TextField cMove = new TextField(String.valueOf(settings.carnivoreMoveCost));
        TextField cEat = new TextField(String.valueOf(settings.carnivoreEatGain));
        TextField cRepro = new TextField(String.valueOf(settings.carnivoreReproduceThreshold));

        Button apply = new Button("Apply");
        Button cancel = new Button("Cancel");

        apply.setOnAction(e -> {
            try {
                settings.gridWidth = Integer.parseInt(gridW.getText());
                settings.gridHeight = Integer.parseInt(gridH.getText());

                settings.initialPlants = Integer.parseInt(plants.getText());
                settings.initialHerbivores = Integer.parseInt(herbs.getText());
                settings.initialCarnivores = Integer.parseInt(carn.getText());

                settings.plantEnergy = Integer.parseInt(plantEnergy.getText());
                settings.plantGrowRate = Double.parseDouble(plantGrow.getText());

                settings.herbivoreStartEnergy = Integer.parseInt(hEnergy.getText());
                settings.herbivoreMoveCost = Integer.parseInt(hMove.getText());
                settings.herbivoreEatGain = Integer.parseInt(hEat.getText());
                settings.herbivoreReproduceThreshold = Integer.parseInt(hRepro.getText());

                settings.carnivoreStartEnergy = Integer.parseInt(cEnergy.getText());
                settings.carnivoreMoveCost = Integer.parseInt(cMove.getText());
                settings.carnivoreEatGain = Integer.parseInt(cEat.getText());
                settings.carnivoreReproduceThreshold = Integer.parseInt(cRepro.getText());

                dlg.close();
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Invalid values: " + ex.getMessage());
                a.showAndWait();
            }
        });
        cancel.setOnAction(e -> dlg.close());


        // Chia thông số thành 2 cột
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        // Cột trái
        grid.add(new Label("Grid W:"), 0, 0);
        grid.add(gridW, 1, 0);
        grid.add(new Label("Grid H:"), 0, 1);
        grid.add(gridH, 1, 1);
        grid.add(new Label("Initial plants:"), 0, 2);
        grid.add(plants, 1, 2);
        grid.add(new Label("Initial herbivores:"), 0, 3);
        grid.add(herbs, 1, 3);
        grid.add(new Label("Initial carnivores:"), 0, 4);
        grid.add(carn, 1, 4);
        grid.add(new Label("Plant energy:"), 0, 5);
        grid.add(plantEnergy, 1, 5);
        grid.add(new Label("Plant grow rate (0-1):"), 0, 6);
        grid.add(plantGrow, 1, 6);

        // Cột phải
        grid.add(new Label("Herbivore start energy:"), 2, 0);
        grid.add(hEnergy, 3, 0);
        grid.add(new Label("Herbivore move cost:"), 2, 1);
        grid.add(hMove, 3, 1);
        grid.add(new Label("Herbivore eat gain:"), 2, 2);
        grid.add(hEat, 3, 2);
        grid.add(new Label("Herbivore reproduce threshold:"), 2, 3);
        grid.add(hRepro, 3, 3);
        grid.add(new Label("Carnivore start energy:"), 2, 4);
        grid.add(cEnergy, 3, 4);
        grid.add(new Label("Carnivore move cost:"), 2, 5);
        grid.add(cMove, 3, 5);
        grid.add(new Label("Carnivore eat gain:"), 2, 6);
        grid.add(cEat, 3, 6);
        grid.add(new Label("Carnivore reproduce threshold:"), 2, 7);
        grid.add(cRepro, 3, 7);

        // ScrollPane cho toàn bộ grid
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(340);

        // Nút Apply/Cancel ở dưới
        HBox btnBox = new HBox(16, apply, cancel);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(12, 16, 12, 16));


        VBox v = new VBox(8, scroll, btnBox);
        v.setPrefWidth(600);
        v.setAlignment(Pos.CENTER);
        scroll.setStyle("-fx-background-color: transparent;");
        btnBox.setAlignment(Pos.CENTER);
        StackPane stack = new StackPane(v);
        stack.setPrefSize(700, 500);
        StackPane.setAlignment(v, Pos.CENTER);
        dlg.setScene(new Scene(stack));
        dlg.show();

        // After dialog close, reset simulation with new settings
        engine = new SimulationEngine(settings);
        canvas.setWidth(engine.grid.getWidth() * cellSize);
        canvas.setHeight(engine.grid.getHeight() * cellSize);
        drawGrid();
        statsLabel.setText(formatCounts());
        updateSummary.run();
        updateDetail.run();
    }

    // ...existing code...

    private String formatCounts() {
        java.util.Map<String, Integer> counts = engine.counts();
        return String.format("Plants: %d  Herbivores: %d  Carnivores: %d",
            counts.getOrDefault("Plant", 0),
            counts.getOrDefault("Herbivore", 0),
            counts.getOrDefault("Carnivore", 0));
    }

    /**
     * 
     */
    private void drawGrid() {
        GraphicsContext g = canvas.getGraphicsContext2D();
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
                java.util.List<Organism> objs = engine.grid.organismsAt(x, y);
                if (objs.isEmpty()) continue;
                String name = objs.get(0).getType();
                Image img = iconMap.get(name);
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

    private void loadIcons() {  
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
                    try {
                        found = new Image(p.toUri().toString());
                        break;
                    } catch (Exception ex) {}
                }
                p = base.resolve(n.toLowerCase() + ".png");
                if (Files.exists(p)) {
                    try {
                        found = new Image(p.toUri().toString());
                        break;
                    } catch (Exception ex) {}
                }
            }
            if (found != null) iconMap.put(n, found);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
