/**
 * UI orchestrator: builds main menu and simulation scenes,
 * handles environment rendering, controls, and selection updates.
 */
package ecosystem.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import ecosystem.models.Organism;
import ecosystem.models.OrganismSnapshot;

/**
 * Manages the JavaFX UI for the ecosystem simulation.
 * JavaFXApp delegates to this class so that its start() method
 * stays small and focused on high-level logic.
 */
public class SimulationUIManager {

    private final AppController controller;

    private int cellSize = 20;
    private Timeline timeline;
    private Timeline menuTimeline;
    // removed legacy menuTimeline; use menuScanTimeline for menu animation
    private Timeline menuScanTimeline;
    private Scene menuScene;
    private Scene simulationScene;
    private File currentFile = null;
    private List<File> recentFiles = new ArrayList<>();
    private Canvas canvas;
    private Canvas menuCanvas;
    private Color[][] menuEnvColors;
    private int menuScanRow;
    private boolean menuScanDown;
    private IntegerProperty overviewGridW = new SimpleIntegerProperty(0);
    private IntegerProperty overviewGridH = new SimpleIntegerProperty(0);
    private Label statsLabel;
    private ListView<OrganismSnapshot> orgListView;
    // per-cell environment map used as background for the simulation grid
    private Color[][] envMap = null;

    // For property panel
    private Label detailLabel;
    private Runnable updateSummary;
    private Runnable updateDetail;

    public SimulationUIManager(AppController controller) {
        this.controller = controller;
    }

    /**
     * Build and show the main menu + simulation scenes.
     */
    public void init(Stage primaryStage) {
        // initialize selection manager (null = nothing selected)
        controller.getSelectionManager().clear();
        IconLoader.loadIcons(controller);

        // Ensure app icons exist (generate if missing) and set stage icon
        try { IconUtil.ensureAppIcons(primaryStage); } catch (Exception ex) { /* ignore */ }

        // MenuBar (Top)

        MenuBar menuBar = MenuFactory.createMenuBar(controller, this::drawGrid, this::applyTheme, this::regenerateEnvironment);

        // Extracted panels: create instances early so file-menu handlers can call update routines
        OverviewPanel overviewPanel = new OverviewPanel(controller);
        RightPanel rightPanelObj = new RightPanel(controller);
        orgListView = rightPanelObj.getOrgListView();
        detailLabel = rightPanelObj.getDetailLabel();
        updateSummary = () -> { rightPanelObj.updateSummary(); overviewPanel.drawOverview(); };
        updateDetail = () -> rightPanelObj.updateDetailForSelection();

        // --- File menu actions (New / Open / Save / Save As) ---
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ecosim files", "*.ecosim", "*.txt"));
        Menu menuFile = menuBar.getMenus().get(0);
        MenuItem newItem = new MenuItem("New");
        MenuItem openItem = new MenuItem("Open...");
        MenuItem saveItem = new MenuItem("Save");
        MenuItem saveAsItem = new MenuItem("Save As...");
        MenuItem exitItem = new MenuItem("Exit");
        menuFile.getItems().addAll(newItem, openItem, saveItem, saveAsItem, new SeparatorMenuItem(), exitItem);

        newItem.setOnAction(e -> {
            if (timeline != null) timeline.stop();
            controller.setSettings(new ecosystem.Settings());
            controller.setEngine(new ecosystem.logic.SimulationEngine(controller.getSettings()));
            controller.getSelectionManager().clear();
            canvas.setWidth(controller.getEngine().getGrid().getWidth() * cellSize);
            canvas.setHeight(controller.getEngine().getGrid().getHeight() * cellSize);
            overviewGridW.set(controller.getSettings().getGridWidth());
            overviewGridH.set(controller.getSettings().getGridHeight());
            // regenerate environment and terrain map for new grid
            EnvironmentGenerator.EnvironmentData env = EnvironmentGenerator.generateEnvironment(
                controller.getEngine().getGrid().getWidth(),
                controller.getEngine().getGrid().getHeight());
            envMap = env.colors;
            controller.getEngine().getGrid().setTerrain(env.terrain);
            controller.getEngine().getGrid().ensureOrganismsOnValidTerrain();
            updateSummary.run(); drawGrid(); statsLabel.setText(formatCounts()); overviewPanel.drawOverview(); updateDetail.run();
            currentFile = null;
        });

        openItem.setOnAction(e -> {
            File f = chooser.showOpenDialog(primaryStage);
            if (f == null) return;
            try {
                controller.loadFromFile(f);
                // rebind listeners and update UI
                canvas.setWidth(controller.getEngine().getGrid().getWidth() * cellSize);
                canvas.setHeight(controller.getEngine().getGrid().getHeight() * cellSize);
                overviewGridW.set(controller.getSettings().getGridWidth());
                overviewGridH.set(controller.getSettings().getGridHeight());
                // re-register listener
                controller.getEngine().getGrid().addListener(new ecosystem.models.Grid.GridListener() {
                    @Override public void organismAdded(Organism o) { Platform.runLater(() -> { if (orgListView == null) return; OrganismSnapshot s = OrganismSnapshot.from(o); javafx.collections.ObservableList<OrganismSnapshot> items = orgListView.getItems(); int idx = 0; while (idx < items.size() && items.get(idx).id < s.id) idx++; items.add(idx, s); }); }
                    @Override public void organismRemoved(Organism o) { Platform.runLater(() -> { if (orgListView == null) return; javafx.collections.ObservableList<OrganismSnapshot> items = orgListView.getItems(); items.removeIf(x -> x.id == o.getId()); Integer sel = controller.getSelectionManager().getSelectedIdProperty().get(); if (sel != null && sel == o.getId()) controller.getSelectionManager().clear(); }); }
                    @Override public void organismUpdated(Organism o) { Platform.runLater(() -> { if (orgListView == null) return; javafx.collections.ObservableList<OrganismSnapshot> items = orgListView.getItems(); for (int i = 0; i < items.size(); i++) { if (items.get(i).id == o.getId()) { items.set(i, OrganismSnapshot.from(o)); break; } } }); }
                });
                // regenerate environment & terrain map after loading
                EnvironmentGenerator.EnvironmentData env = EnvironmentGenerator.generateEnvironment(
                    controller.getEngine().getGrid().getWidth(),
                    controller.getEngine().getGrid().getHeight());
                envMap = env.colors;
                controller.getEngine().getGrid().setTerrain(env.terrain);
                updateSummary.run(); drawGrid(); statsLabel.setText(formatCounts()); overviewPanel.drawOverview(); updateDetail.run();
                currentFile = f;
                // update recent list
                recentFiles.remove(f);
                recentFiles.add(0, f);
                if (recentFiles.size() > 6) recentFiles.remove(recentFiles.size() - 1);
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert a = new Alert(Alert.AlertType.ERROR, "Failed to open file: " + ex.getMessage(), ButtonType.OK);
                a.initOwner(primaryStage);
                a.showAndWait();
            }
        });

        saveItem.setOnAction(e -> {
            try {
                if (currentFile == null) {
                    File f = chooser.showSaveDialog(primaryStage);
                    if (f == null) return;
                    controller.saveToFile(f);
                    currentFile = f;
                    recentFiles.remove(f);
                    recentFiles.add(0, f);
                    if (recentFiles.size() > 6) recentFiles.remove(recentFiles.size() - 1);
                } else {
                    controller.saveToFile(currentFile);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert a = new Alert(Alert.AlertType.ERROR, "Failed to save file: " + ex.getMessage(), ButtonType.OK);
                a.initOwner(primaryStage);
                a.showAndWait();
            }
        });

        saveAsItem.setOnAction(e -> {
            try {
                File f = chooser.showSaveDialog(primaryStage);
                if (f == null) return;
                controller.saveToFile(f);
                currentFile = f;
                recentFiles.remove(f);
                recentFiles.add(0, f);
                if (recentFiles.size() > 6) recentFiles.remove(recentFiles.size() - 1);
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert a = new Alert(Alert.AlertType.ERROR, "Failed to save file: " + ex.getMessage(), ButtonType.OK);
                a.initOwner(primaryStage);
                a.showAndWait();
            }
        });

        exitItem.setOnAction(e -> primaryStage.close());

        // Left: Overview + TreeView (from OverviewPanel)
        VBox leftPanel = overviewPanel.getRoot();

        // Center: Canvas lớn
        canvas = new Canvas(controller.getEngine().getGrid().getWidth() * cellSize, controller.getEngine().getGrid().getHeight() * cellSize);
        // generate environment & terrain map for initial grid
        EnvironmentGenerator.EnvironmentData initialEnv = EnvironmentGenerator.generateEnvironment(
            controller.getEngine().getGrid().getWidth(),
            controller.getEngine().getGrid().getHeight());
        envMap = initialEnv.colors;
        controller.getEngine().getGrid().setTerrain(initialEnv.terrain);
        controller.getEngine().getGrid().ensureOrganismsOnValidTerrain();
        drawGrid();

        // Prepare settings dialog action which will reset engine and update UI when applied
        Runnable onSettingsApplied = () -> {
            controller.resetEngine();
            canvas.setWidth(controller.getEngine().getGrid().getWidth() * cellSize);
            canvas.setHeight(controller.getEngine().getGrid().getHeight() * cellSize);
            overviewGridW.set(controller.getSettings().getGridWidth());
            overviewGridH.set(controller.getSettings().getGridHeight());
            // regenerate environment & terrain map for the new grid
            EnvironmentGenerator.EnvironmentData env = EnvironmentGenerator.generateEnvironment(
                controller.getEngine().getGrid().getWidth(),
                controller.getEngine().getGrid().getHeight());
            envMap = env.colors;
            controller.getEngine().getGrid().setTerrain(env.terrain);
            controller.getEngine().getGrid().ensureOrganismsOnValidTerrain();
            updateSummary.run();
            drawGrid();
            statsLabel.setText(formatCounts());
            overviewPanel.drawOverview();
            updateDetail.run();
        };

        // Right panel and control panel
        VBox rightPanel = rightPanelObj.getRoot();
        HBox controls = createControlPanel(primaryStage, onSettingsApplied);

        // Register grid listener to perform incremental updates to the organisms list
        controller.getEngine().getGrid().addListener(new ecosystem.models.Grid.GridListener() {
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
        simRoot.getStyleClass().add("sim-root");
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
        }

        simulationScene = new Scene(simRoot, 1300, 700);
        // base stylesheet (common rules)
        simulationScene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        // apply current theme (will add light/dark stylesheet)
        applyTheme();

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
        controls.getStyleClass().add("control-bar");
        startBtn.getStyleClass().add("control-button");
        pauseBtn.getStyleClass().add("control-button");
        stepBtn.getStyleClass().add("control-button");
        resetBtn.getStyleClass().add("control-button");
        settingsBtn.getStyleClass().add("control-button");
        backBtn.getStyleClass().add("control-button");
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
            // Keep the current terrain so Reset only re-randomizes organisms
            // while preserving the existing environment (water/land layout).
            int[][] oldTerrain = null;
            if (controller.getEngine() != null && controller.getEngine().getGrid() != null) {
                oldTerrain = controller.getEngine().getGrid().getTerrain();
            }
            controller.resetEngine();
            canvas.setWidth(controller.getEngine().getGrid().getWidth() * cellSize);
            canvas.setHeight(controller.getEngine().getGrid().getHeight() * cellSize);
            // Reapply previous terrain (if any) so environment stays the same
            if (oldTerrain != null) {
                controller.getEngine().getGrid().setTerrain(oldTerrain);
                // Ensure newly created organisms are not in water
                controller.getEngine().getGrid().ensureOrganismsOnValidTerrain();
            }
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
            int cols = controller.getEngine().getGrid().getWidth();
            int rows = controller.getEngine().getGrid().getHeight();
            double cellW = canvas.getWidth() / (double) Math.max(1, cols);
            double cellH = canvas.getHeight() / (double) Math.max(1, rows);
            int gx = (int) Math.floor(e.getX() / cellW);
            int gy = (int) Math.floor(e.getY() / cellH);
            if (gx >= 0 && gx < cols && gy >= 0 && gy < rows) {
                java.util.List<Organism> objs = controller.getEngine().getGrid().organismsAt(gx, gy);
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

    private void regenerateEnvironment() {
        int cols = controller.getEngine().getGrid().getWidth();
        int rows = controller.getEngine().getGrid().getHeight();
        EnvironmentGenerator.EnvironmentData env = EnvironmentGenerator.generateEnvironment(cols, rows);
        envMap = env.colors;
        controller.getEngine().getGrid().setTerrain(env.terrain);
        drawGrid();
    }

    private void drawGrid() {
        if (canvas == null || controller.getEngine() == null) return;
        GraphicsContext g = canvas.getGraphicsContext2D();
        int cols = controller.getEngine().getGrid().getWidth();
        int rows = controller.getEngine().getGrid().getHeight();
        double w = canvas.getWidth() / Math.max(1, cols);
        double h = canvas.getHeight() / Math.max(1, rows);
        if (controller.isUseImageBackground() && controller.getGridBackgroundImage() != null) {
            g.drawImage(controller.getGridBackgroundImage(), 0, 0, canvas.getWidth(), canvas.getHeight());
        } else if (envMap != null && envMap.length == cols && envMap[0].length == rows) {
            // draw per-cell environment "pixel" background
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    Color c = envMap[x][y];
                    g.setFill(c);
                    g.fillRect(x * w, y * h, w, h);
                }
            }
        } else {
            // fallback gradient background when envMap not available
            javafx.scene.paint.LinearGradient lg = new javafx.scene.paint.LinearGradient(
                0, 0, 1, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#f7fbf9")),
                new javafx.scene.paint.Stop(1, Color.web("#eef6ef"))
            );
            g.setFill(lg);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            // soft vignette overlay
            javafx.scene.paint.RadialGradient rg = new javafx.scene.paint.RadialGradient(
                0, 0.1, canvas.getWidth() / 2, canvas.getHeight() / 2,
                Math.max(canvas.getWidth(), canvas.getHeight()) * 0.7, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.color(0,0,0,0.0)),
                new javafx.scene.paint.Stop(1, Color.color(0,0,0,0.06))
            );
            g.setFill(rg);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
        // subtle grid lines for visual structure
        g.setStroke(Color.web("#e8e8e8", 0.9));
        g.setLineWidth(0.6);
        for (int i = 1; i < cols; i++) {
            double x = i * w + 0.5;
            g.strokeLine(x, 0, x, canvas.getHeight());
        }
        for (int j = 1; j < rows; j++) {
            double y = j * h + 0.5;
            g.strokeLine(0, y, canvas.getWidth(), y);
        }
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                java.util.List<Organism> objs = controller.getEngine().getGrid().organismsAt(x, y);
                if (objs.isEmpty()) continue;
                String name = objs.get(0).getType();
                Image img = controller.getIconMap().get(name);
                if (img != null) {
                    g.drawImage(img, x * w, y * h, w, h);
                } else {
                    switch (name) {
                        case "Plant": g.setFill(Color.web("#7fbf7f")); break;
                        case "Herbivore": g.setFill(Color.web("#ffb84d")); break;
                        case "Carnivore": g.setFill(Color.web("#ff6b6b")); break;
                        default: g.setFill(Color.BLACK); break;
                    }
                    g.fillRect(x * w, y * h, w, h);
                }
                // Always draw a white border around animals (Herbivore, Carnivore)
                if ("Herbivore".equals(name) || "Carnivore".equals(name)) {
                    g.setStroke(Color.WHITE);
                    double borderWidth = Math.max(1, Math.min(3, Math.min(w, h) * 0.15));
                    g.setLineWidth(borderWidth);
                    double inset = borderWidth * 0.5;
                    g.strokeRect(x * w + inset, y * h + inset, w - 2 * inset, h - 2 * inset);
                }

                if ("Plant".equals(name)) {
                    // For plants, draw a semi-transparent dark green border
                    g.setStroke(Color.web("#004d00", 0.6));
                    double borderWidth = Math.max(1, Math.min(2, Math.min(w, h) * 0.1));
                    g.setLineWidth(borderWidth);
                    double inset = borderWidth * 0.5;
                    g.strokeRect(x * w + inset, y * h + inset, w - 2 * inset, h - 2 * inset);
                }
            }
        }

        // If an organism is selected, just highlight it. The
        // detail text is managed centrally via RightPanel
        // (through updateDetail.run()).
        Integer selId = controller.getSelectionManager().getSelectedIdProperty().get();
        if (selId != null) {
            Organism o = controller.getEngine().getGrid().getOrganismById(selId);
            if (o != null) {
                g.setStroke(Color.BLACK);
                g.setLineWidth(Math.max(1, Math.min(4, (float) (Math.min(w, h) * 0.08))));
                g.strokeRect(o.getX() * w, o.getY() * h, w, h);
            }
        }
    }

    private void showMainMenu(Stage stage, Runnable onStart, Runnable onSettings) {
        StackPane root = new StackPane();
        // Animated environment background
        menuCanvas = new Canvas(1300, 700);
        menuCanvas.widthProperty().bind(root.widthProperty());
        menuCanvas.heightProperty().bind(root.heightProperty());
        root.getChildren().add(menuCanvas);

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

        startBtn.setOnAction(e -> { if (menuTimeline != null) menuTimeline.stop(); if (onStart != null) onStart.run(); });
        settingsBtn.setOnAction(e -> { if (onSettings != null) onSettings.run(); });
        aboutBtn.setOnAction(e -> showAboutDialog(stage));
        helpBtn.setOnAction(e -> showHelpDialog(stage));
        exitBtn.setOnAction(e -> { if (menuTimeline != null) menuTimeline.stop(); stage.close(); });

        menuBox.getChildren().addAll(title, startBtn, settingsBtn, aboutBtn, helpBtn, exitBtn);
        root.getChildren().add(menuBox);

        // Scanning animation: regenerate env then reveal rows top→bottom or bottom→top slowly
        Runnable[] startScanHolder = new Runnable[1];
        startScanHolder[0] = () -> {
            double w = Math.max(1, menuCanvas.getWidth());
            double h = Math.max(1, menuCanvas.getHeight());
            int cols = Math.max(24, (int)Math.round(w / 22));
            int rows = Math.max(14, (int)Math.round(h / 22));
            EnvironmentGenerator.EnvironmentData env = EnvironmentGenerator.generateEnvironment(cols, rows);
            menuEnvColors = env.colors;
            // clear canvas before scan
            GraphicsContext g = menuCanvas.getGraphicsContext2D();
            g.setFill(Color.web("#000000ff"));
            g.fillRect(0, 0, menuCanvas.getWidth(), menuCanvas.getHeight());
            // randomize direction
            menuScanDown = Math.random() < 0.5;
            menuScanRow = menuScanDown ? 0 : rows - 1;
            if (menuScanTimeline != null) menuScanTimeline.stop();
            menuScanTimeline = new Timeline(new KeyFrame(Duration.millis(70), e2 -> {
                drawMenuEnvironmentRow(menuCanvas, menuEnvColors, menuScanRow);
                menuScanRow += menuScanDown ? 1 : -1;
                if (menuScanRow < 0 || menuScanRow >= rows) {
                    // add gentle overlay and pause before next scan
                    drawMenuOverlay(menuCanvas);
                    menuScanTimeline.stop();
                    Timeline pause = new Timeline(new KeyFrame(Duration.millis(800), ee -> startScanHolder[0].run()));
                    pause.setCycleCount(1);
                    pause.play();
                }
            }));
            menuScanTimeline.setCycleCount(Timeline.INDEFINITE);
            menuScanTimeline.play();
        };
        startScanHolder[0].run();

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

    private void drawMenuEnvironmentRow(Canvas c, Color[][] colors, int rowIndex) {
        if (c == null || colors == null || rowIndex < 0) return;
        int cols = colors.length;
        int rows = cols > 0 ? colors[0].length : 0;
        if (cols == 0 || rows == 0 || rowIndex >= rows) return;
        GraphicsContext g = c.getGraphicsContext2D();
        double cellW = c.getWidth() / (double) cols;
        double cellH = c.getHeight() / (double) rows;
        for (int x = 0; x < cols; x++) {
            g.setFill(colors[x][rowIndex]);
            g.fillRect(x * cellW, rowIndex * cellH, cellW, cellH);
        }
    }

    private void drawMenuOverlay(Canvas c) {
        GraphicsContext g = c.getGraphicsContext2D();
        g.setStroke(Color.color(0,0,0,0.06));
        g.setLineWidth(1);
        for (double yy = 0; yy < c.getHeight(); yy += 3) {
            g.strokeLine(0, yy, c.getWidth(), yy);
        }
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

    private void applyTheme() {
        if (simulationScene == null)
            return;
        // remove any existing theme-specific stylesheets
        simulationScene.getStylesheets().removeIf(s -> s.endsWith("styles_light.css") || s.endsWith("styles_dark.css"));
        // ensure base stylesheet present
        String base = getClass().getResource("styles.css").toExternalForm();
        if (!simulationScene.getStylesheets().contains(base))
            simulationScene.getStylesheets().add(base);
        // add selected theme
        if (controller != null && controller.isDarkTheme()) {
            simulationScene.getStylesheets().add(getClass().getResource("styles_dark.css").toExternalForm());
        } else {
            simulationScene.getStylesheets().add(getClass().getResource("styles_light.css").toExternalForm());
        }
    }
}
