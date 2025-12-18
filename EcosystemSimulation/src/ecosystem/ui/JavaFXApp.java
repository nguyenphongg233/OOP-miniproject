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
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
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
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.application.Platform;
import ecosystem.ui.SelectionManager;
import ecosystem.models.OrganismSnapshot;
import java.util.HashMap;
import java.util.Map;

public class JavaFXApp extends Application {

    // Instance variables
    private AppController controller;
    private int cellSize = 20;
    private Timeline timeline;
    private Scene menuScene;
    private Scene simulationScene;
    private Canvas canvas;
    private Canvas overviewCanvas;
    private IntegerProperty overviewGridW = new SimpleIntegerProperty(0);
    private IntegerProperty overviewGridH = new SimpleIntegerProperty(0);
    private Label statsLabel;
    private ListView<OrganismSnapshot> orgListView;
    
    // prevent recursive selection updates between ListView and selectedId
    private boolean updatingSelection = false;
    
    // For property panel
    private Label detailLabel;
    private Runnable updateSummary;
    private Runnable updateDetail;

    @Override
    public void start(Stage primaryStage) {
        controller = new AppController();
        // initialize selection manager (null = nothing selected)
        controller.getSelectionManager().clear();
        IconLoader.loadIcons(controller);

        // MenuBar (Top)
        MenuBar menuBar = MenuFactory.createMenuBar(controller, () -> drawGrid());
        // Left: Overview + TreeView
        VBox leftPanel = OverviewPanel();
        // Center: Canvas lớn
        canvas = new Canvas(controller.getEngine().grid.getWidth() * cellSize, controller.getEngine().grid.getHeight() * cellSize);
        drawGrid();

        // Prepare settings dialog action which will reset engine and update UI when applied
        Runnable onSettingsApplied = () -> {
            controller.resetEngine();
            canvas.setWidth(controller.getEngine().grid.getWidth() * cellSize);
            canvas.setHeight(controller.getEngine().grid.getHeight() * cellSize);
            overviewGridW.set(controller.getSettings().getGridWidth());
            overviewGridH.set(controller.getSettings().getGridHeight());
            updateSummary.run();
            drawGrid();
            statsLabel.setText(formatCounts());
            drawOverview();
            updateDetail.run();
        };

        // Right panel and control panel are created by helper methods
        VBox rightPanel = createRightPanel();
        HBox controls = createControlPanel(primaryStage, onSettingsApplied);

        // Register grid listener to perform incremental updates to the organisms list
        controller.getEngine().grid.addListener(new ecosystem.models.Grid.GridListener() {
            @Override public void organismAdded(Organism o) {
                Platform.runLater(() -> {
                    if (orgListView == null) return;
                    OrganismSnapshot s = OrganismSnapshot.from(o);
                    javafx.collections.ObservableList<OrganismSnapshot> items = orgListView.getItems();
                    int idx = 0; while (idx < items.size() && items.get(idx).id < s.id) idx++;
                    items.add(idx, s);
                });
            }
            @Override public void organismRemoved(Organism o) {
                Platform.runLater(() -> {
                    if (orgListView == null) return;
                    javafx.collections.ObservableList<OrganismSnapshot> items = orgListView.getItems();
                    items.removeIf(x -> x.id == o.getId());
                    Integer sel = controller.getSelectionManager().getSelectedIdProperty().get();
                    if (sel != null && sel == o.getId()) controller.getSelectionManager().clear();
                });
            }
            @Override public void organismUpdated(Organism o) {
                Platform.runLater(() -> {
                    if (orgListView == null) return;
                    javafx.collections.ObservableList<OrganismSnapshot> items = orgListView.getItems();
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i).id == o.getId()) { items.set(i, OrganismSnapshot.from(o)); break; }
                    }
                });
            }
        });

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
        // If a grid background image was loaded earlier, use it; otherwise keep the plain BEIGE background
        
        
        if (controller.getSimRootBackgroundImage() != null) {
            BackgroundImage bgImg = new BackgroundImage(
                controller.getSimRootBackgroundImage(),
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1.0, 1.0, true, true, false, true)
            );
            System.out.println("Using simulation background image");
            simRoot.setBackground(new Background(bgImg));
        } else {
            simRoot.setBackground(new Background(new BackgroundFill(
                    Color.BLANCHEDALMOND, CornerRadii.EMPTY, Insets.EMPTY
            )));
        }

        simulationScene = new Scene(simRoot, 1300, 700);

        // --- Menu Scene ---
        showMainMenu(primaryStage,
            () -> {
                primaryStage.setScene(simulationScene);
                primaryStage.setTitle("Ecosystem Simulation (JavaFX)");
            },
            () -> SettingsDialog.open(primaryStage, controller, onSettingsApplied)
        );
        menuScene = primaryStage.getScene();
    }
    
    private VBox OverviewPanel() {
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(8));
        leftPanel.setPrefWidth(180);
        // Overview canvas: bind its width to left panel and height to maintain grid aspect ratio
        overviewCanvas = new Canvas(160, 160);
        overviewCanvas.widthProperty().bind(leftPanel.widthProperty().subtract(16));
        // Bind height = width * (gridHeight / gridWidth)
        overviewCanvas.heightProperty().bind(Bindings.createDoubleBinding(() -> {
            double w = overviewCanvas.getWidth();
            int gw = Math.max(1, overviewGridW.get());
            int gh = Math.max(1, overviewGridH.get());
            return w * ((double) gh / (double) gw);
        }, overviewCanvas.widthProperty(), overviewGridW, overviewGridH));
        overviewCanvas.getGraphicsContext2D().setFill(Color.LIGHTGRAY);
        overviewCanvas.getGraphicsContext2D().fillRect(0, 0, 160, 160);
        // Redraw overview when size changes
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
        // Initialize overview grid size from settings and draw
        overviewGridW.set(controller.getSettings().getGridWidth());
        overviewGridH.set(controller.getSettings().getGridHeight());
        drawOverview();
        return leftPanel;
    }

    /**
     * Draw a scaled overview of the simulation grid into overviewCanvas.
     * Colors: Plant = light green, Herbivore = dark gray, Carnivore = black.
     */
    private void drawOverview() {
        if (overviewCanvas == null || controller.getEngine() == null) return;
        GraphicsContext g = overviewCanvas.getGraphicsContext2D();
        int cols = controller.getEngine().grid.getWidth();
        int rows = controller.getEngine().grid.getHeight();
        double w = overviewCanvas.getWidth();
        double h = overviewCanvas.getHeight();
        if (w <= 0 || h <= 0 || cols <= 0 || rows <= 0) return;
        g.setFill(Color.web("#f0f0f0"));
        g.fillRect(0, 0, w, h);
        double cellW = w / cols;
        double cellH = h / rows;
        // If grid is larger than canvas, we will sample cells to fit
        double scale = Math.min(cellW, cellH);
        // compute number of columns/rows to draw based on scale
        int drawCols = (int)Math.min(cols, Math.max(1, Math.floor(w / scale)));
        int drawRows = (int)Math.min(rows, Math.max(1, Math.floor(h / scale)));
        double drawCellW = w / drawCols;
        double drawCellH = h / drawRows;
        for (int ry = 0; ry < drawRows; ry++) {
            for (int rx = 0; rx < drawCols; rx++) {
                // map draw cell to grid coordinate (sample)
                int gx = (int)((double)rx * cols / drawCols);
                int gy = (int)((double)ry * rows / drawRows);
                java.util.List<Organism> objs = controller.getEngine().grid.organismsAt(gx, gy);
                if (objs.isEmpty()) {
                    // empty cell
                    g.setFill(Color.web("#e9efe9"));
                } else {
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
    

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(8));
        rightPanel.setPrefWidth(220);
        Label orgListLabel = new Label("Organisms");
        orgListView = new ListView<>();
        orgListView.setPrefHeight(160);
        orgListView.setPrefWidth(200);
        // Show Organism label in each cell
        orgListView.setCellFactory(lv -> new ListCell<OrganismSnapshot>() {
            @Override protected void updateItem(OrganismSnapshot o, boolean empty) {
                super.updateItem(o, empty);
                if (empty || o == null) setText(null);
                else setText(o.labelId);
            }
        });

        // ListView -> SelectionManager (guarded)
        orgListView.getSelectionModel().selectedItemProperty().addListener((obs, oldO, newO) -> {
            if (updatingSelection) return;
            if (newO == null) {
                controller.getSelectionManager().clear();
                drawGrid();
                return;
            }
            controller.getSelectionManager().select(newO.id);
            drawGrid();
        });

        // SelectionManager -> ListView (guarded)
        controller.getSelectionManager().getSelectedIdProperty().addListener((obs, oldId, newId) -> {
            if (updatingSelection) return;
            updatingSelection = true;
            try {
                if (orgListView != null) {
                    if (newId == null) {
                        orgListView.getSelectionModel().clearSelection();
                    } else {
                        for (OrganismSnapshot o : orgListView.getItems()) {
                            if (o.id == newId.intValue()) {
                                orgListView.getSelectionModel().select(o);
                                orgListView.scrollTo(o);
                                break;
                            }
                        }
                    }
                }
                drawGrid();
                updateDetail.run();
            } finally {
                updatingSelection = false;
            }
        });
        Label propLabel = new Label("Properties");
        VBox propBox = new VBox(8);
        propBox.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 8; -fx-border-radius: 8; -fx-background-radius: 8;");
        Label dayLabel = new Label();
        Label plantLabel = new Label();
        Label herbLabel = new Label();
        Label carnLabel = new Label();
        Label orgLabel = new Label();
        detailLabel = new Label("Click a cell to view organism details");
        detailLabel.setWrapText(true);
        propBox.getChildren().addAll(dayLabel, plantLabel, herbLabel, carnLabel, orgLabel, new Separator(), detailLabel);
        rightPanel.getChildren().addAll(orgListLabel, orgListView, propLabel, propBox);
        updateSummary = () -> {
                java.util.Map<String, Integer> counts = controller.getEngine().counts();
            dayLabel.setText("Day: " + controller.getEngine().getDay() + " (Step: " + controller.getEngine().getStep() + ")");
            plantLabel.setText("Plants: " + counts.getOrDefault("Plant", 0));
            herbLabel.setText("Herbivores: " + counts.getOrDefault("Herbivore", 0));
            carnLabel.setText("Carnivores: " + counts.getOrDefault("Carnivore", 0));
            int total = 0;
            for (int v : counts.values()) total += v;
            orgLabel.setText("Total Organisms: " + total);
            // update overview minimap whenever summary updates
            drawOverview();
            // update organisms list (id: type (x,y))
                if (orgListView != null) {
                javafx.collections.ObservableList<OrganismSnapshot> items = orgListView.getItems();
                // replace entire list with current organisms (snapshots)
                java.util.List<OrganismSnapshot> current = new java.util.ArrayList<>();
                for (Organism o : controller.getEngine().grid.getOrganisms()) current.add(OrganismSnapshot.from(o));
                // Keep a stable ordering (by id) so selection doesn't jump around when grid reorders
                current.sort((a, b) -> Integer.compare(a.id, b.id));
                // replace entire list with current snapshots while guarding selection updates
                updatingSelection = true;
                try {
                    items.setAll(current);
                    Integer selId = controller.getSelectionManager().getSelectedIdProperty().get();
                    boolean foundSelected = false;
                    if (selId != null) {
                        for (OrganismSnapshot o : items) if (o.id == selId) { foundSelected = true; break; }
                    }
                    if (!foundSelected && selId != null) {
                        controller.getSelectionManager().clear();
                        orgListView.getSelectionModel().clearSelection();
                        detailLabel.setText("Click a cell to view organism details");
                    } else if (foundSelected && selId != null) {
                        for (OrganismSnapshot o : items) {
                            if (o.id == selId) { orgListView.getSelectionModel().select(o); break; }
                        }
                    }
                } finally {
                    updatingSelection = false;
                }
            }
        };
        updateSummary.run();
        updateDetail = () -> {
            detailLabel.setText("Click a cell to view organism details");
        };
        return rightPanel;
    }

    private HBox createControlPanel(Stage primaryStage, Runnable onSettingsApplied) {
        Button startBtn = new Button("Start");
        Button pauseBtn = new Button("Pause");
        Button stepBtn = new Button("Step");
        Button resetBtn = new Button("Reset");
        Button settingsBtn = new Button("Settings");
        Button backBtn = new Button("Back to Menu");
        Slider speed = new Slider(10, 1000, 200);
        speed.setShowTickLabels(true);
        speed.setShowTickMarks(true);
        statsLabel = new Label(formatCounts());
        HBox controls = new HBox(8, startBtn, pauseBtn, stepBtn, resetBtn, settingsBtn, backBtn, new Label("Delay ms:"), speed);
        controls.setPadding(new Insets(8));
        controls.setStyle("-fx-background-color: #ffffffcc; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, #b2ebf2, 8, 0.2, 0, 2)");
        String btnStyle = "-fx-background-radius: 16; -fx-effect: dropshadow(gaussian, #80deea, 8, 0.3, 0, 2); -fx-font-size: 14px; -fx-padding: 8 20;";
        startBtn.setStyle(btnStyle);
        pauseBtn.setStyle(btnStyle);
        stepBtn.setStyle(btnStyle);
        resetBtn.setStyle(btnStyle);
        settingsBtn.setStyle(btnStyle);
        backBtn.setStyle(btnStyle);
        timeline = new Timeline(new KeyFrame(Duration.millis(speed.getValue()), e -> {
            controller.getEngine().tick();
            updateSummary.run();
            drawGrid();
            statsLabel.setText(formatCounts());
            updateDetail.run();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        speed.valueProperty().addListener((obs, oldV, newV) -> timeline.setRate(1000.0 / newV.doubleValue()));
        startBtn.setOnAction(e -> timeline.play());
        pauseBtn.setOnAction(e -> timeline.pause());
        stepBtn.setOnAction(e -> {
            controller.getEngine().tick();
            updateSummary.run();
            drawGrid();
            statsLabel.setText(formatCounts());
            updateDetail.run();
        });
        resetBtn.setOnAction(e -> {
            timeline.stop();
            controller.resetEngine();
            canvas.setWidth(controller.getEngine().grid.getWidth() * cellSize);
            canvas.setHeight(controller.getEngine().grid.getHeight() * cellSize);
            updateSummary.run();
            drawGrid();
            statsLabel.setText(formatCounts());
            updateDetail.run();
        });
        settingsBtn.setOnAction(e -> SettingsDialog.open(primaryStage, controller, onSettingsApplied));
        backBtn.setOnAction(e -> {
            timeline.stop();
            primaryStage.setScene(menuScene);
            primaryStage.setTitle("Ecosystem Simulation — Main Menu");
        });
        canvas.widthProperty().addListener((o, oldV, newV) -> drawGrid());
        canvas.heightProperty().addListener((o, oldV, newV) -> drawGrid());
        canvas.setOnMouseClicked(e -> {
            // compute grid coords using current canvas scale (not fixed cellSize)
            int cols = controller.getEngine().grid.getWidth();
            int rows = controller.getEngine().grid.getHeight();
            double cellW = canvas.getWidth() / (double) Math.max(1, cols);
            double cellH = canvas.getHeight() / (double) Math.max(1, rows);
            int gx = (int) Math.floor(e.getX() / cellW);
            int gy = (int) Math.floor(e.getY() / cellH);
            if (gx >= 0 && gx < cols && gy >= 0 && gy < rows) {
                java.util.List<Organism> objs = controller.getEngine().grid.organismsAt(gx, gy);
                if (!objs.isEmpty()) {
                    // select the top organism at the clicked cell (selection manager listener will update list)
                    Organism o = objs.get(0);
                    controller.getSelectionManager().select(o.getId());
                    drawGrid();
                } else {
                    // click on empty cell -> clear selection
                    controller.getSelectionManager().clear();
                    if (orgListView != null) orgListView.getSelectionModel().clearSelection();
                    drawGrid();
                }
            }
        });
        return controls;
    }

    private String formatCounts() {
        java.util.Map<String, Integer> counts = controller.getEngine().counts();
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
        int cols = controller.getEngine().grid.getWidth();
        int rows = controller.getEngine().grid.getHeight();
        double w = canvas.getWidth() / cols;
        double h = canvas.getHeight() / rows;
        if (controller.isUseImageBackground() && controller.getGridBackgroundImage() != null) {
            g.drawImage(controller.getGridBackgroundImage(), 0, 0, canvas.getWidth(), canvas.getHeight());
        } else {
            g.setFill(Color.BEIGE);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                java.util.List<Organism> objs = controller.getEngine().grid.organismsAt(x, y);
                if (objs.isEmpty()) continue;
                String name = objs.get(0).getType();
                Image img = controller.getIconMap().get(name);
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
        // If an organism is selected in the list, highlight it with a black border

        Integer selId = controller.getSelectionManager().getSelectedIdProperty().get();
        if (selId != null) {
            Organism o = controller.getEngine().grid.getOrganismById(selId);
            if (o != null) {
                g.setStroke(Color.BLACK);
                g.setLineWidth(Math.max(1, Math.min(4, (float) (Math.min(w, h) * 0.08))));
                g.strokeRect(o.getX() * w, o.getY() * h, w, h);
            }
        }
        // Update detail panel based on current selection (moved from click handler)
        if (selId != null) {
            Organism sel = controller.getEngine().grid.getOrganismById(selId);
            if (sel != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Type: ").append(sel.getType()).append("\n");
                sb.append("Position: (").append(sel.getX()).append(", ").append(sel.getY()).append(")\n");
                sb.append("Age: ").append(sel.getAge()).append("\n");
                sb.append("Energy: ").append(sel.getEnergy()).append("\n");
                sb.append("Alive: ").append(sel.isAlive() ? "Yes" : "No").append("\n");
                sb.append("ID: ").append(sel.getId()).append("\n");
                detailLabel.setText(sb.toString());
                updateDetail = () -> detailLabel.setText(sb.toString());
            } else {
                // selected organism not found (died/removed) — clear selection and detail
                controller.getSelectionManager().clear();
                if (orgListView != null) orgListView.getSelectionModel().clearSelection();
                detailLabel.setText("Click a cell to view organism details");
                updateDetail = () -> detailLabel.setText("Click a cell to view organism details");
            }
        }
    }

    

    public static void main(String[] args) {
        launch(args);
    }

    // selection state is stored in the `selectedId` property declared at top

    /**
     * Show the main menu as a scene on the provided stage.
     */
    private void showMainMenu(Stage stage, Runnable onStart, Runnable onSettings) {
        StackPane root = new StackPane();

        // Load background image if exists
        Path bgPath = Path.of("EcosystemSimulation", "icons", "background.png");
        if (!Files.exists(bgPath)) bgPath = Path.of("icons", "background.png");
        if (Files.exists(bgPath)) {
            Image bgImg = new Image(bgPath.toUri().toString());
            BackgroundImage bg = new BackgroundImage(
                bgImg,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1.0, 1.0, true, true, false, true)
            );
            root.setBackground(new Background(bg));
        } else {
            root.setStyle("-fx-background-color: linear-gradient(to bottom,#e0f7fa,#b2ebf2);");
        }

        VBox menuBox = new VBox(18);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setMaxWidth(340);

        Label title = new Label("Ecosystem Simulation");
        title.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#3ddfafff"));
        title.setStyle("-fx-effect: dropshadow(gaussian, #fefefeff, 8, 0.5, 0, 2);");

        Button startBtn = createMenuButton("Start Simulation");
        Button settingsBtn = createMenuButton("Settings");
        Button aboutBtn = createMenuButton("About");
        Button helpBtn = createMenuButton("Help");
        Button exitBtn = createMenuButton("Exit");

        startBtn.setOnAction(e -> { if (onStart != null) onStart.run(); });
        settingsBtn.setOnAction(e -> { if (onSettings != null) onSettings.run(); });
        aboutBtn.setOnAction(e -> showAboutDialog(stage));
        helpBtn.setOnAction(e -> showHelpDialog(stage));
        exitBtn.setOnAction(e -> stage.close());

        menuBox.getChildren().addAll(title, startBtn, settingsBtn, aboutBtn, helpBtn, exitBtn);

        root.getChildren().add(menuBox);

        Scene scene = new Scene(root, 1300, 700);
        scene.widthProperty().addListener((obs, oldV, newV) -> {
            double w = newV.doubleValue();
            double btnW = Math.max(180, Math.min(320, w * 0.45));
            for (javafx.scene.Node n : menuBox.getChildren()) {
                if (n instanceof Button) ((Button)n).setPrefWidth(btnW);
            }
        });
        scene.heightProperty().addListener((obs, oldV, newV) -> {
            double h = newV.doubleValue();
            menuBox.setSpacing(Math.max(14, Math.min(32, h * 0.04)));
        });

        stage.setScene(scene);
        stage.setTitle("Ecosystem Simulation — Main Menu");
        stage.show();
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.SEMI_BOLD, 20));
        btn.setStyle("-fx-background-color: #69bdacff; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 10 0; -fx-effect: dropshadow(gaussian, #80cbc4, 6, 0.3, 0, 2);");
        btn.setPrefHeight(44);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #70772cff; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 10 0; -fx-effect: dropshadow(gaussian, #b2dfdb, 8, 0.5, 0, 2);"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #69bdacff; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 10 0; -fx-effect: dropshadow(gaussian, #80cbc4, 6, 0.3, 0, 2);"));
        return btn;
    }

    private void showAboutDialog(Stage owner) {
        Stage dlg = new Stage();
        dlg.initOwner(owner);
        dlg.setTitle("About");
        VBox v = new VBox(16,
            new Label("Ecosystem Simulation"),
            new Label("Version 1.0"),
            new Label("Developed by HUST Team")
        );
        v.setAlignment(Pos.CENTER);
        v.setStyle("-fx-padding: 32;");
        dlg.setScene(new Scene(v, 320, 180));
        dlg.show();
    }

    private void showHelpDialog(Stage owner) {
        Stage dlg = new Stage();
        dlg.initOwner(owner);
        dlg.setTitle("Help");
        VBox v = new VBox(16,
            new Label("- Start Simulation: Bắt đầu mô phỏng hệ sinh thái."),
            new Label("- Settings: Cài đặt thông số mô phỏng."),
            new Label("- About: Thông tin ứng dụng."),
            new Label("- Exit: Thoát chương trình.")
        );
        v.setAlignment(Pos.CENTER_LEFT);
        v.setStyle("-fx-padding: 32;");
        dlg.setScene(new Scene(v, 400, 220));
        dlg.show();
    }

}
