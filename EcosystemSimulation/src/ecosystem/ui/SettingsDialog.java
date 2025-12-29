/**
 * Modal settings dialog: allows tweaking `Settings` and applies changes via callback.
 */
package ecosystem.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ecosystem.Settings;

public class SettingsDialog {
    public static void open(Stage owner, AppController c, Runnable onApplied) {
        Stage dlg = new Stage();
        dlg.initOwner(owner);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Settings");
        Settings settings = c.getSettings();

        TextField gridW = new TextField(String.valueOf(settings.getGridWidth()));
        TextField gridH = new TextField(String.valueOf(settings.getGridHeight()));
        TextField plants = new TextField(String.valueOf(settings.getInitialPlants()));
        TextField herbs = new TextField(String.valueOf(settings.getInitialHerbivores()));
        TextField carn = new TextField(String.valueOf(settings.getInitialCarnivores()));

        TextField plantEnergy = new TextField(String.valueOf(settings.getPlantEnergy()));
        TextField plantGrow = new TextField(String.valueOf(settings.getPlantGrowRate()));

        TextField hEnergy = new TextField(String.valueOf(settings.getHerbivoreStartEnergy()));
        TextField hMove = new TextField(String.valueOf(settings.getHerbivoreMoveCost()));
        TextField hEat = new TextField(String.valueOf(settings.getHerbivoreEatGain()));
        TextField hRepro = new TextField(String.valueOf(settings.getHerbivoreReproduceThreshold()));

        TextField cEnergy = new TextField(String.valueOf(settings.getCarnivoreStartEnergy()));
        TextField cMove = new TextField(String.valueOf(settings.getCarnivoreMoveCost()));
        TextField cEat = new TextField(String.valueOf(settings.getCarnivoreEatGain()));
        TextField cRepro = new TextField(String.valueOf(settings.getCarnivoreReproduceThreshold()));

        Button apply = new Button("Apply");
        Button cancel = new Button("Cancel");

        apply.setOnAction(e -> {
            try {
                settings.setGridWidth(Integer.parseInt(gridW.getText()));
                settings.setGridHeight(Integer.parseInt(gridH.getText()));

                settings.setInitialPlants(Integer.parseInt(plants.getText()));
                settings.setInitialHerbivores(Integer.parseInt(herbs.getText()));
                settings.setInitialCarnivores(Integer.parseInt(carn.getText()));

                settings.setPlantEnergy(Integer.parseInt(plantEnergy.getText()));
                settings.setPlantGrowRate(Double.parseDouble(plantGrow.getText()));

                settings.setHerbivoreStartEnergy(Integer.parseInt(hEnergy.getText()));
                settings.setHerbivoreMoveCost(Integer.parseInt(hMove.getText()));
                settings.setHerbivoreEatGain(Integer.parseInt(hEat.getText()));
                settings.setHerbivoreReproduceThreshold(Integer.parseInt(hRepro.getText()));

                settings.setCarnivoreStartEnergy(Integer.parseInt(cEnergy.getText()));
                settings.setCarnivoreMoveCost(Integer.parseInt(cMove.getText()));
                settings.setCarnivoreEatGain(Integer.parseInt(cEat.getText()));
                settings.setCarnivoreReproduceThreshold(Integer.parseInt(cRepro.getText()));

                dlg.close();
                // reset engine and notify caller
                c.resetEngine();
                if (onApplied != null) onApplied.run();
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Invalid values: " + ex.getMessage());
                a.showAndWait();
            }
        });
        cancel.setOnAction(e -> dlg.close());

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

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

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(340);

        HBox btnBox = new HBox(16, apply, cancel);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(12, 16, 12, 16));

        VBox v = new VBox(8, scroll, btnBox);
        v.setPrefWidth(600);
        v.setAlignment(Pos.CENTER);
        StackPane stack = new StackPane(v);
        stack.setPrefSize(700, 500);
        StackPane.setAlignment(v, Pos.CENTER);
        dlg.setScene(new Scene(stack));
        dlg.show();
    }
}
