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
import java.util.HashMap;
import java.util.Map;

public class JavaFXApp extends Application {

    // Instance variables
    private SimulationEngine engine;
    private Settings settings;
    private int cellSize = 20;
    private Timeline timeline;
    private Scene menuScene;
    private Scene simulationScene;
    private Canvas canvas;
    private Canvas overviewCanvas;
    private IntegerProperty overviewGridW = new SimpleIntegerProperty(0);
    private IntegerProperty overviewGridH = new SimpleIntegerProperty(0);
    private Label statsLabel;
    private ListView<Organism> orgListView;
    private Integer selectedOrganismId = null;
    private Map<String, Image> iconMap = new HashMap<>();
    private Image gridBackgroundImage = null, simRootBackgroundImage = null;
    private boolean useImageBackground = false;
    // For property panel
    private Label detailLabel;
    private Runnable updateSummary;
    private Runnable updateDetail;

    @Override
    public void start(Stage primaryStage) {
        settings = new Settings();
        engine = new SimulationEngine(settings);
        // restore legacy selection-by-id variable
        selectedOrganismId = null;
        loadIcons();

        // MenuBar (Top)
        MenuBar menuBar = createMenuBar();
        // Left: Overview + TreeView
        VBox leftPanel = OverviewPanel();
        // Center: Canvas lớn
        canvas = new Canvas(engine.grid.getWidth() * cellSize, engine.grid.getHeight() * cellSize);
        drawGrid();

        // Right panel and control panel are created by helper methods
        VBox rightPanel = createRightPanel();
        HBox controls = createControlPanel(primaryStage);

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
        
        
        if (simRootBackgroundImage != null) {
            BackgroundImage bgImg = new BackgroundImage(
                simRootBackgroundImage,
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
    private MenuBar createMenuBar() {
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
        bgImgItem.setOnAction(e -> {
            useImageBackground = true;
            drawGrid();
        });
        bgColorItem.setOnAction(e -> {
            useImageBackground = false;
            drawGrid();
        });
        return menuBar;
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
        overviewGridW.set(settings.gridWidth);
        overviewGridH.set(settings.gridHeight);
        drawOverview();
        return leftPanel;
    }

    /**
     * Draw a scaled overview of the simulation grid into overviewCanvas.
     * Colors: Plant = light green, Herbivore = dark gray, Carnivore = black.
     */
    private void drawOverview() {
        if (overviewCanvas == null || engine == null) return;
        GraphicsContext g = overviewCanvas.getGraphicsContext2D();
        int cols = engine.grid.getWidth();
        int rows = engine.grid.getHeight();
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
                java.util.List<Organism> objs = engine.grid.organismsAt(gx, gy);
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
        // Update overview grid bindings first
        overviewGridW.set(settings.gridWidth);
        overviewGridH.set(settings.gridHeight);
        // Refresh summary (which also redraws overview), then draw grid
        updateSummary.run();
        drawGrid();
        statsLabel.setText(formatCounts());
        drawOverview();
        updateDetail.run();
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
        orgListView.setCellFactory(lv -> new ListCell<Organism>() {
            @Override protected void updateItem(Organism o, boolean empty) {
                super.updateItem(o, empty);
                if (empty || o == null) setText(null);
                else setText(o.getLabelId());
            }
        });
        orgListView.getSelectionModel().selectedItemProperty().addListener((obs, oldO, newO) -> {
            if (newO == null) {
                selectedOrganismId = null;
                drawGrid();
                return;
            }
            selectedOrganismId = newO.getId();
            drawGrid();
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
            java.util.Map<String, Integer> counts = engine.counts();
            dayLabel.setText("Day: " + engine.getDay() + " (Step: " + engine.getStep() + ")");
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
                javafx.collections.ObservableList<Organism> items = orgListView.getItems();
                // replace entire list with current organisms
                java.util.List<Organism> current = new java.util.ArrayList<>(engine.grid.getOrganisms());
                // Keep a stable ordering (by id) so selection doesn't jump around when grid reorders
                current.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
                items.setAll(current);
                boolean foundSelected = false;
                Integer selId = selectedOrganismId;
                if (selId != null) {
                    for (Organism o : items) if (o.getId() == selId) { foundSelected = true; break; }
                }
                // If selected organism no longer exists, clear selection and detail
                if (!foundSelected && selId != null) {
                    selectedOrganismId = null;
                    orgListView.getSelectionModel().clearSelection();
                    detailLabel.setText("Click a cell to view organism details");
                } else if (foundSelected && selId != null) {
                    // restore selection to the matching organism object
                    for (Organism o : items) {
                        if (o.getId() == selId) {
                            orgListView.getSelectionModel().select(o);
                            break;
                        }
                    }
                }
            }
        };
        updateSummary.run();
        updateDetail = () -> {
            detailLabel.setText("Click a cell to view organism details");
        };
        return rightPanel;
    }

    private HBox createControlPanel(Stage primaryStage) {
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
            engine.tick();
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
            engine.tick();
            updateSummary.run();
            drawGrid();
            statsLabel.setText(formatCounts());
            updateDetail.run();
        });
        resetBtn.setOnAction(e -> {
            timeline.stop();
            engine = new SimulationEngine(settings);
            canvas.setWidth(engine.grid.getWidth() * cellSize);
            canvas.setHeight(engine.grid.getHeight() * cellSize);
            updateSummary.run();
            drawGrid();
            statsLabel.setText(formatCounts());
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
        canvas.setOnMouseClicked(e -> {
            // compute grid coords using current canvas scale (not fixed cellSize)
            int cols = engine.grid.getWidth();
            int rows = engine.grid.getHeight();
            double cellW = canvas.getWidth() / (double) Math.max(1, cols);
            double cellH = canvas.getHeight() / (double) Math.max(1, rows);
            int gx = (int) Math.floor(e.getX() / cellW);
            int gy = (int) Math.floor(e.getY() / cellH);
            if (gx >= 0 && gx < cols && gy >= 0 && gy < rows) {
                java.util.List<Organism> objs = engine.grid.organismsAt(gx, gy);
                if (!objs.isEmpty()) {
                    // select the top organism at the clicked cell
                    Organism o = objs.get(0);
                    selectedOrganismId = o.getId();
                    // select and focus the matching item in the list so behavior matches panel selection
                    if (orgListView != null) {
                        for (Organism item : orgListView.getItems()) {
                            if (item.getId() == o.getId()) {
                                orgListView.getSelectionModel().select(item);
                                orgListView.scrollTo(item);
                                orgListView.requestFocus();
                                break;
                            }
                        }
                    }
                    drawGrid();
                } else {
                    // click on empty cell -> clear selection
                    selectedOrganismId = null;
                    if (orgListView != null) orgListView.getSelectionModel().clearSelection();
                    drawGrid();
                }
            }
        });
        return controls;
    }

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
        // If an organism is selected in the list, highlight it with a black border

        Integer selId = selectedOrganismId;
        if (selId != null) {
            for (Organism o : engine.grid.getOrganisms()) {
                if (o.getId() == selId) {
                    g.setStroke(Color.BLACK);
                    g.setLineWidth(Math.max(1, Math.min(4, (float) (Math.min(w, h) * 0.08)))) ;
                    g.strokeRect(o.getX() * w, o.getY() * h, w, h);
                    break;
                }
            }
        }
        // Update detail panel based on current selection (moved from click handler)
        if (selId != null) {
            Organism sel = null;
            for (Organism o : engine.grid.getOrganisms()) {
                if (o.getId() == selId) {
                    sel = o;
                    break;
                }
            }
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
                selectedOrganismId = null;
                if (orgListView != null) orgListView.getSelectionModel().clearSelection();
                detailLabel.setText("Click a cell to view organism details");
                updateDetail = () -> detailLabel.setText("Click a cell to view organism details");
            }
        }
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
        Path bgPath = Path.of("EcosystemSimulation", "icons", "grid_background.png");
        if (Files.exists(bgPath)) {
            try {
                gridBackgroundImage = new Image(bgPath.toUri().toString());
            } catch (Exception ex) { gridBackgroundImage = null; }
        }
        Path simBgPath = Path.of("EcosystemSimulation", "icons", "sim_background.png");
        if (Files.exists(simBgPath)) {
            try {
                simRootBackgroundImage = new Image(simBgPath.toUri().toString());
            } catch (Exception ex) { simRootBackgroundImage = null; }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
