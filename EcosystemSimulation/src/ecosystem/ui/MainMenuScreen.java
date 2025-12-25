package ecosystem.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Builds and controls the retro main menu screen and its
 * background BFS animation.
 */
public class MainMenuScreen {

    // Main menu retro background animation (BFS reveal)
    private Timeline menuBackgroundTimeline;
    private int menuBgCols = 80;
    private int menuBgRows = 45;
    private Color[][] menuBgEnv;
    private boolean[][] menuBgVisited;
    private Queue<int[]> menuBgQueue;

    public void show(Stage stage, Runnable onStart, Runnable onSettings) {
        StackPane root = new StackPane();

        // Retro main menu background: no image, just CRT-like gradient and frame.
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #000000, #101820); " +
                      "-fx-border-color: #40c8f4; -fx-border-width: 2px;");

        // Canvas to render animated BFS-based environment background
        Canvas bgCanvas = new Canvas(1300, 700);
        root.getChildren().add(bgCanvas);

        VBox menuBox = new VBox(18);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setMaxWidth(340);

        Label title = new Label("Ecosystem Simulation");
        title.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#e0f8d0"));
        title.setStyle("-fx-padding: 8 0 16 0; -fx-border-color: #40c8f4; -fx-border-width: 0 0 2 0;");

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

        // Fit background canvas to scene and start BFS animation
        bgCanvas.widthProperty().bind(scene.widthProperty());
        bgCanvas.heightProperty().bind(scene.heightProperty());
        startMenuBackgroundAnimation(bgCanvas);

        stage.setScene(scene);
        stage.setTitle("Ecosystem Simulation  Main Menu");
        stage.show();
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text.toUpperCase());
        btn.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, 18));
        btn.setStyle("-fx-background-color: #202830; -fx-text-fill: #e0f8d0; -fx-background-radius: 0; -fx-padding: 8 0; -fx-border-color: #40c8f4; -fx-border-width: 2;");
        btn.setPrefHeight(40);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2a3644; -fx-text-fill: #e0f8d0; -fx-background-radius: 0; -fx-padding: 8 0; -fx-border-color: #40c8f4; -fx-border-width: 2;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #202830; -fx-text-fill: #e0f8d0; -fx-background-radius: 0; -fx-padding: 8 0; -fx-border-color: #40c8f4; -fx-border-width: 2;"));
        return btn;
    }

    /**
     * Animate a retro environment being revealed by BFS on the main menu.
     * The animation loops: when all cells have been drawn, a new environment
     * is generated and the reveal restarts from the center.
     */
    private void startMenuBackgroundAnimation(Canvas bgCanvas) {
        if (menuBackgroundTimeline != null) {
            menuBackgroundTimeline.stop();
        }

        GraphicsContext g = bgCanvas.getGraphicsContext2D();

        Runnable initState = () -> {
            EnvironmentGenerator.EnvironmentData env = EnvironmentGenerator.generateEnvironment(menuBgCols, menuBgRows);
            menuBgEnv = env.colors;
            menuBgVisited = new boolean[menuBgCols][menuBgRows];
            menuBgQueue = new ArrayDeque<>();
            int sx = menuBgCols / 2;
            int sy = menuBgRows / 2;
            menuBgVisited[sx][sy] = true;
            menuBgQueue.add(new int[]{sx, sy});
            // clear background
            g.setFill(Color.BLACK);
            g.fillRect(0, 0, bgCanvas.getWidth(), bgCanvas.getHeight());
        };

        initState.run();

        // Slower, more relaxed retro animation
        menuBackgroundTimeline = new Timeline(new KeyFrame(Duration.millis(80), e -> {
            if (menuBgEnv == null || menuBgQueue == null) return;
            double cellW = bgCanvas.getWidth() / (double) menuBgCols;
            double cellH = bgCanvas.getHeight() / (double) menuBgRows;

            int stepsPerFrame = 40;
            int processed = 0;
            while (!menuBgQueue.isEmpty() && processed < stepsPerFrame) {
                int[] cell = menuBgQueue.poll();
                int x = cell[0], y = cell[1];
                Color c = menuBgEnv[x][y];
                g.setFill(c);
                g.fillRect(x * cellW, y * cellH, cellW + 1, cellH + 1);

                // enqueue 4-neighbors (BFS)
                int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
                for (int[] d : dirs) {
                    int nx = x + d[0];
                    int ny = y + d[1];
                    if (nx >= 0 && nx < menuBgCols && ny >= 0 && ny < menuBgRows && !menuBgVisited[nx][ny]) {
                        menuBgVisited[nx][ny] = true;
                        menuBgQueue.add(new int[]{nx, ny});
                    }
                }
                processed++;
            }

            // if finished, restart with a new environment
            if (menuBgQueue.isEmpty()) {
                initState.run();
            }

            // overlay light scanlines to keep CRT feel
            g.setStroke(Color.color(0, 0, 0, 0.2));
            g.setLineWidth(1);
            for (double y = 0; y < bgCanvas.getHeight(); y += 2) {
                g.strokeLine(0, y, bgCanvas.getWidth(), y);
            }
        }));
        menuBackgroundTimeline.setCycleCount(Timeline.INDEFINITE);
        menuBackgroundTimeline.play();
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
