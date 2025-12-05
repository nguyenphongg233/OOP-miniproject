package ecosystem.ui;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainMenu {
    private Stage stage;
    private Runnable onStart;
    private Runnable onSettings;

    public MainMenu(Stage stage, Runnable onStart, Runnable onSettings) {
        this.stage = stage;
        this.onStart = onStart;
        this.onSettings = onSettings;
    }

    public void show() {
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
        //menuBox.setStyle("-fx-background-color: rgba(35, 92, 105, 0.85); -fx-background-radius: 18; -fx-padding: 32;");

        Label title = new Label("Ecosystem Simulation");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#3ddfafff"));
        title.setStyle("-fx-effect: dropshadow(gaussian, #fefefeff, 8, 0.5, 0, 2);");

        Button startBtn = createMenuButton("Start Simulation");
        Button settingsBtn = createMenuButton("Settings");
        Button aboutBtn = createMenuButton("About");
        Button helpBtn = createMenuButton("Help");
        Button exitBtn = createMenuButton("Exit");

        startBtn.setOnAction(e -> { if (onStart != null) onStart.run(); });
        settingsBtn.setOnAction(e -> { if (onSettings != null) onSettings.run(); });
        aboutBtn.setOnAction(e -> showAboutDialog());
        helpBtn.setOnAction(e -> showHelpDialog());
        exitBtn.setOnAction(e -> stage.close());

        menuBox.getChildren().addAll(title, startBtn, settingsBtn, aboutBtn, helpBtn, exitBtn);

        root.getChildren().add(menuBox);

        Scene scene = new Scene(root, 600, 420);
        // Responsive: menuBox always center, buttons scale with window
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
        btn.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 20));
        btn.setStyle("-fx-background-color: #69bdacff; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 10 0; -fx-effect: dropshadow(gaussian, #80cbc4, 6, 0.3, 0, 2);");
        btn.setPrefHeight(44);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #70772cff; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 10 0; -fx-effect: dropshadow(gaussian, #b2dfdb, 8, 0.5, 0, 2);"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #69bdacff; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 10 0; -fx-effect: dropshadow(gaussian, #80cbc4, 6, 0.3, 0, 2);"));
        return btn;
    }

    private void showAboutDialog() {
        Stage dlg = new Stage();
        dlg.initOwner(stage);
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

    private void showHelpDialog() {
        Stage dlg = new Stage();
        dlg.initOwner(stage);
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
