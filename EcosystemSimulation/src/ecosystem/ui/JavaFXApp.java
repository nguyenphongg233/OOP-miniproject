package ecosystem.ui;

import javafx.application.Application;
import javafx.stage.Stage;
// icon/image helpers moved to IconUtil

public class JavaFXApp extends Application {

    private AppController controller;
    private SimulationUIManager uiManager;

    @Override
    public void start(Stage primaryStage) {
        controller = new AppController();
        uiManager = new SimulationUIManager(controller);
        uiManager.init(primaryStage);
    }
    public static void main(String[] args) {
        launch(args);
    }
}
